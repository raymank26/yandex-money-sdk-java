/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 NBCO Yandex.Money LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yandex.money.api.methods;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.yandex.money.api.model.Error;
import com.yandex.money.api.model.ExternalCard;
import com.yandex.money.api.net.HostsProvider;
import com.yandex.money.api.net.MethodRequest;
import com.yandex.money.api.net.PostRequestBodyBuffer;
import com.yandex.money.api.utils.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Process external payment.
 *
 * @author Dmitriy Melnikov (dvmelnikov@yamoney.ru)
 */
public class ProcessExternalPayment extends BaseProcessPayment {

    public final ExternalCard moneySource; // TODO rename it to externalCard

    /**
     * Constructor.
     *
     * @param moneySource money source info if asked for a money source token
     *
     * @see com.yandex.money.api.methods.BaseProcessPayment
     */
    public ProcessExternalPayment(Status status, Error error, String invoiceId, String acsUri,
                                  Map<String, String> acsParams, Long nextRetry,
                                  ExternalCard moneySource) {
        super(status, error, invoiceId, acsUri, acsParams, nextRetry);
        this.moneySource = moneySource;
    }

    /**
     * Constructor.
     *
     * @param moneySource money source info if asked for a money source token
     * @param invoiceId invoice id of successful operation
     *
     * @see com.yandex.money.api.methods.BaseProcessPayment
     */
    @Deprecated
    public ProcessExternalPayment(Status status, Error error, String acsUri,
                                  Map<String, String> acsParams, ExternalCard moneySource,
                                  Long nextRetry, String invoiceId) {
        this(status, error, invoiceId, acsUri, acsParams, nextRetry, moneySource);
    }

    @Override
    public String toString() {
        return super.toString() + "ProcessExternalPayment{" +
                "moneySource=" + moneySource +
                ", invoiceId='" + invoiceId + '\'' +
                '}';
    }

    /**
     * Request for processing external payment.
     */
    public static class Request implements MethodRequest<ProcessExternalPayment> {

        private final String instanceId;
        private final String requestId;
        private final String extAuthSuccessUri;
        private final String extAuthFailUri;
        private final boolean requestToken;
        private final ExternalCard externalCard;
        private final String csc;

        /**
         * For paying with a new card.
         *
         * @param instanceId application's instance id
         * @param requestId request id from {@link com.yandex.money.api.methods.RequestExternalPayment}
         * @param extAuthSuccessUri success URI to use if payment succeeded
         * @param extAuthFailUri fail URI to use if payment failed
         * @param requestToken {@code true} if money source token is required
         */
        public Request(String instanceId, String requestId, String extAuthSuccessUri,
                       String extAuthFailUri, boolean requestToken) {
            this(instanceId, requestId, extAuthSuccessUri, extAuthFailUri, requestToken, null, null);
        }

        /**
         * For paying with a saved card.
         *
         * @param instanceId application's instance id
         * @param requestId request id from {@link com.yandex.money.api.methods.RequestExternalPayment}
         * @param extAuthSuccessUri success URI to use if payment succeeded
         * @param extAuthFailUri fail URI to use if payment failed
         * @param externalCard money source token of a saved card
         * @param csc Card Security Code for a saved card.
         */
        public Request(String instanceId, String requestId, String extAuthSuccessUri,
                       String extAuthFailUri, ExternalCard externalCard, String csc) {
            this(instanceId, requestId, extAuthSuccessUri, extAuthFailUri, false, externalCard,
                    csc);
        }

        private Request(String instanceId, String requestId, String extAuthSuccessUri,
                        String extAuthFailUri, boolean requestToken, ExternalCard externalCard,
                        String csc) {

            if (Strings.isNullOrEmpty(instanceId))
                throw new IllegalArgumentException("instanceId is null or empty");
            this.instanceId = instanceId;

            if (Strings.isNullOrEmpty(requestId))
                throw new IllegalArgumentException("requestId is null or empty");
            this.requestId = requestId;

            if (Strings.isNullOrEmpty(extAuthSuccessUri))
                throw new IllegalArgumentException("extAuthSuccessUri is null or empty");
            this.extAuthSuccessUri = extAuthSuccessUri;

            if (Strings.isNullOrEmpty(extAuthFailUri))
                throw new IllegalArgumentException("extAuthFailUri is null or empty");
            this.extAuthFailUri = extAuthFailUri;

            if (requestToken) {
                this.requestToken = true;
                this.externalCard = null;
                this.csc = null;
            } else {
                this.requestToken = false;
                this.externalCard = externalCard;
                this.csc = csc;
            }
        }

        @Override
        public URL requestURL(HostsProvider hostsProvider) throws MalformedURLException {
            return new URL(hostsProvider.getMoneyApi() + "/process-external-payment");
        }

        @Override
        public ProcessExternalPayment parseResponse(InputStream inputStream) {
            return new GsonBuilder().registerTypeAdapter(ProcessExternalPayment.class, new JsonDeserializer<ProcessExternalPayment>() {
                @Override
                public ProcessExternalPayment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonObject o = json.getAsJsonObject();

                    JsonObject paramsObj = o.getAsJsonObject(MEMBER_ACS_PARAMS);
                    Map<String, String> acsParams = paramsObj == null ?
                            new HashMap<String, String>() : JsonUtils.map(paramsObj);

                    final String moneySourceMember = "money_source";
                    ExternalCard moneySource = o.has(moneySourceMember) ?
                            ExternalCard.createFromJson(o.get(moneySourceMember)) : null;

                    return new ProcessExternalPayment(
                            Status.parse(JsonUtils.getString(o, MEMBER_STATUS)),
                            Error.parse(JsonUtils.getString(o, MEMBER_ERROR)),
                            JsonUtils.getString(o, "invoice_id"),
                            JsonUtils.getString(o, MEMBER_ACS_URI),
                            acsParams,
                            JsonUtils.getLong(o, MEMBER_NEXT_RETRY),
                            moneySource
                    );
                }
            }).create().fromJson(new InputStreamReader(inputStream), ProcessExternalPayment.class);
        }

        @Override
        public PostRequestBodyBuffer buildParameters() throws IOException {
            PostRequestBodyBuffer bb = new PostRequestBodyBuffer();

            bb.addParam("instance_id", instanceId);
            bb.addParam("request_id", requestId);
            bb.addParam("ext_auth_success_uri", extAuthSuccessUri);
            bb.addParam("ext_auth_fail_uri", extAuthFailUri);

            bb.addBooleanIfTrue("request_token", requestToken);
            if (externalCard != null) {
                bb.addParamIfNotNull("money_source_token", externalCard.moneySourceToken);
                bb.addParamIfNotNull("csc", csc);
            }

            return bb;
        }
    }
}
