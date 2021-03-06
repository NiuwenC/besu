/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import org.hyperledger.besu.ethereum.api.jsonrpc.JsonRpcEnclaveErrorConverter;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.privacy.methods.PrivacySendTransaction;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.privacy.methods.PrivacySendTransaction.ErrorResponseException;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.privacy.PrivacyController;
import org.hyperledger.besu.ethereum.privacy.PrivateTransaction;
import org.hyperledger.besu.ethereum.privacy.SendTransactionResponse;

import java.util.Base64;

import org.apache.tuweni.bytes.Bytes;

public class PrivDistributeRawTransaction implements JsonRpcMethod {

  private final PrivacyController privacyController;
  private final PrivacySendTransaction privacySendTransaction;
  private final EnclavePublicKeyProvider enclavePublicKeyProvider;

  public PrivDistributeRawTransaction(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    this.privacyController = privacyController;
    this.privacySendTransaction =
        new PrivacySendTransaction(privacyController, enclavePublicKeyProvider);
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_DISTRIBUTE_RAW_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final PrivateTransaction privateTransaction;
    try {
      privateTransaction = privacySendTransaction.validateAndDecodeRequest(requestContext);
    } catch (ErrorResponseException e) {
      return e.getResponse();
    }

    final SendTransactionResponse sendTransactionResponse;
    try {
      sendTransactionResponse =
          privacyController.sendTransaction(
              privateTransaction, enclavePublicKeyProvider.getEnclaveKey(requestContext.getUser()));
    } catch (final Exception e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(),
          JsonRpcEnclaveErrorConverter.convertEnclaveInvalidReason(e.getMessage()));
    }

    return privacySendTransaction.validateAndExecute(
        requestContext,
        privateTransaction,
        sendTransactionResponse.getPrivacyGroupId(),
        () ->
            new JsonRpcSuccessResponse(
                requestContext.getRequest().getId(),
                Bytes.wrap(Base64.getDecoder().decode(sendTransactionResponse.getEnclaveKey()))
                    .toHexString()));
  }
}
