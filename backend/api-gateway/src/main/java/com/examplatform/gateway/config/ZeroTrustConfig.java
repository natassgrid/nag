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

package com.examplatform.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Zero Trust inter-service authentication configuration.
 * <p>
 * Architecture decisions:
 * <ul>
 *   <li>All downstream service calls are authenticated via service-account JWTs
 *       obtained from Keycloak using the client_credentials grant.</li>
 *   <li>No shared secrets, API keys, or static tokens are used between services.</li>
 *   <li>Service-account tokens are short-lived and cached until near expiry by
 *       {@code com.examplatform.shared.auth.ServiceAccountTokenProvider}.</li>
 *   <li>Outgoing requests are intercepted by
 *       {@code com.examplatform.shared.auth.ServiceAuthInterceptor} which
 *       attaches the Bearer token to the Authorization header.</li>
 * </ul>
 * <p>
 * Production fallback: In Kubernetes/Istio deployments, mutual TLS (mTLS) is
 * configured at the service mesh level via Istio's PeerAuthentication policy.
 * mTLS provides transport-layer identity verification complementing the
 * application-layer JWT authentication. Both mechanisms operate simultaneously:
 * <ul>
 *   <li>Istio mTLS ensures the network connection originates from a trusted pod identity (SPIFFE)</li>
 *   <li>Service-account JWTs ensure the calling service has the appropriate authorization claims</li>
 * </ul>
 */
@Configuration
public class ZeroTrustConfig {
    // Configuration is documentation-only.
    // Service-account token authentication is handled by shared-lib components:
    //   - ServiceAccountTokenProvider: manages token lifecycle and caching
    //   - ServiceAuthInterceptor: attaches tokens to outgoing requests
    //
    // In production, Istio provides mTLS as an additional transport-layer security measure.
    // See: infrastructure/k8s/istio/peer-authentication.yaml
}
