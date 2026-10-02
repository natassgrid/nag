// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;
import com.examplatform.practice.dto.AnswerKeyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class QuestionBankClientImpl implements QuestionBankClient {
    private static final Logger log = LoggerFactory.getLogger(QuestionBankClientImpl.class);
    
    @Override
    public Map<UUID, AnswerKeyDto> getAnswerKeys(List<UUID> questionIds) {
        log.debug("Fetching answer keys for {} questions (stub implementation)", questionIds.size());
        return Collections.emptyMap();
    }
}
