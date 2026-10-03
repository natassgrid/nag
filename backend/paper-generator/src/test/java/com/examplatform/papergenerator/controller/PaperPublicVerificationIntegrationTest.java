/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.crypto.MerkleTree;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.MerkleLeafVerifyRequest;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.service.PaperAnchoringService;
import com.examplatform.papergenerator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Paper Public Verification & Merkle Proof Endpoints")
class PaperPublicVerificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private PaperAnchoringService paperAnchoringService;

    private Paper testPaper;
    private UUID q1;
    private UUID q2;

    @BeforeEach
    void setUp() {
        paperRepository.deleteAll();

        q1 = UUID.randomUUID();
        q2 = UUID.randomUUID();

        Paper paper = Paper.builder()
                .name("SSC CGL Tier 1 Set A")
                .examId(UUID.randomUUID())
                .shiftId("SHIFT-1")
                .variant("SET-A")
                .status("DRAFT")
                .isPractice(false)
                .paperDefinitionJson("{\"questionIds\":[\"" + q1 + "\",\"" + q2 + "\"]}")
                .difficultyScore(2.1)
                .topicDistributionJson("{\"Reasoning\":2}")
                .build();
        paper.setTenantId("default");

        testPaper = paperAnchoringService.anchorPaperToLedger(paper, "default");
    }

    @Test
    @DisplayName("Public verification by paperRootHash returns confirmed ledger attestation")
    void testPublicVerificationByHash() throws Exception {
        mockMvc.perform(get("/api/v1/papers/public/verify")
                        .param("examId", testPaper.getExamId().toString())
                        .param("hash", testPaper.getPaperRootHash()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", is(true)))
                .andExpect(jsonPath("$.paperRootHash", is(testPaper.getPaperRootHash())))
                .andExpect(jsonPath("$.ledgerTxHash", notNullValue()))
                .andExpect(jsonPath("$.consensusTimestamp", notNullValue()))
                .andExpect(jsonPath("$.tamperDetected", is(false)));
    }

    @Test
    @DisplayName("Public verification by paperId returns full verification breakdown")
    void testPublicVerificationByPaperId() throws Exception {
        mockMvc.perform(get("/api/v1/papers/public/verify")
                        .param("paperId", testPaper.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified", is(true)))
                .andExpect(jsonPath("$.paperId", is(testPaper.getId().toString())))
                .andExpect(jsonPath("$.totalQuestions", is(2)))
                .andExpect(jsonPath("$.tamperDetected", is(false)));
    }

    @Test
    @DisplayName("Merkle leaf inclusion proof verification succeeds for valid question payload")
    void testMerkleLeafVerificationEndpoint() throws Exception {
        List<String> leaves = List.of(
                "EXAM:e1|VAR:SET-A|IDX:0|Q:" + q1,
                "EXAM:e1|VAR:SET-A|IDX:1|Q:" + q2
        );
        MerkleTree tree = new MerkleTree(leaves);
        MerkleTree.MerkleProof proof = tree.getProof(0);

        MerkleLeafVerifyRequest request = MerkleLeafVerifyRequest.builder()
                .leafHash(proof.getLeafHash())
                .rootHash(tree.getRootHash())
                .proofSteps(proof.getSteps())
                .build();

        mockMvc.perform(post("/api/v1/papers/public/verify/merkle-leaf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.leafHash", is(proof.getLeafHash())))
                .andExpect(jsonPath("$.rootHash", is(tree.getRootHash())));
    }

    @Test
    @DisplayName("Retrieves question inclusion Merkle proof for specific question in paper")
    void testGetQuestionProofEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/papers/public/" + testPaper.getId() + "/merkle-proof/" + q1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootHash", is(testPaper.getPaperRootHash())))
                .andExpect(jsonPath("$.leafIndex", is(0)));
    }
}
