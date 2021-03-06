package com.plaid.client;

import java.util.HashMap;
import java.util.Map;

import com.plaid.client.response.MfaResponse;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.exception.PlaidClientsideException;
import com.plaid.client.exception.PlaidMfaException;
import com.plaid.client.http.HttpDelegate;
import com.plaid.client.http.HttpResponseWrapper;
import com.plaid.client.http.PlaidHttpRequest;
import com.plaid.client.request.ConnectOptions;
import com.plaid.client.request.Credentials;
import com.plaid.client.request.GetOptions;
import com.plaid.client.request.InfoOptions;
import com.plaid.client.response.AccountsResponse;
import com.plaid.client.response.InfoResponse;
import com.plaid.client.response.MessageResponse;
import com.plaid.client.response.PlaidUserResponse;
import com.plaid.client.response.TransactionsResponse;

public class DefaultPlaidUserClient implements PlaidUserClient {

    private String accessToken;

    private String clientId;
    private String secret;

    private ObjectMapper jsonMapper;
    private HttpDelegate httpDelegate;
    private Integer timeout;

    private DefaultPlaidUserClient() {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.setSerializationInclusion(Include.NON_NULL);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    @Override
    public String getAccessToken() {

        return this.accessToken;
    }

    @Override
    public TransactionsResponse addUser(Credentials credentials, String type, String email, ConnectOptions connectOptions) throws PlaidMfaException {

        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("credentials", credentials);
        requestParams.put("type", type);
        requestParams.put("email", email);
        requestParams.put("options", connectOptions);

        return handlePost("/connect", requestParams, TransactionsResponse.class);
    }

    @Override
    public AccountsResponse achAuth(Credentials credentials, String type, ConnectOptions connectOptions) throws PlaidMfaException {

        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("credentials", credentials);
        requestParams.put("type", type);
        requestParams.put("options", connectOptions);

        return handlePost("/auth", requestParams, AccountsResponse.class);
    }

    @Override
    public AccountsResponse getData(){
        Map<String, Object> requestParams = new HashMap<String, Object>();
        return handlePost("/auth/get", requestParams, AccountsResponse.class);
    }

    @Override
    public TransactionsResponse mfaConnectStep(String mfa, String type) throws PlaidMfaException {

        return handleMfa("/connect/step", mfa, type, TransactionsResponse.class);
    }

    @Override
    public AccountsResponse mfaAuthStep(String mfa, String type) throws PlaidMfaException {

        return handleMfa("/auth/step", mfa, type, AccountsResponse.class);
    }

    @Override
    public AccountsResponse mfaAuthDeviceSelectionByDeviceType(String deviceType, String type) throws PlaidMfaException {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        if (StringUtils.isEmpty(deviceType)){
            throw new PlaidClientsideException("No device selected");
        }

        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("type", type);

        HashMap<String, String> mask = new HashMap<String, String>();
        mask.put("type", deviceType);
        HashMap<String, Object> sendMethod = new HashMap<String, Object>();
        sendMethod.put("send_method", mask);
        requestParams.put("options", sendMethod);

        return handlePost("/auth/step", requestParams, AccountsResponse.class);
    }

    @Override
    public AccountsResponse mfaAuthDeviceSelectionByDeviceMask(String deviceMask, String type) throws PlaidMfaException {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        if (StringUtils.isEmpty(deviceMask)) {
            throw new PlaidClientsideException("No device selected");
        }

        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("type", type);

        HashMap<String, String> mask = new HashMap<String, String>();
        mask.put("mask", deviceMask);
        HashMap<String, Object> sendMethod = new HashMap<String, Object>();
        sendMethod.put("send_method", mask);
        requestParams.put("options", sendMethod);

        return handlePost("/auth/step", requestParams, AccountsResponse.class);
    }
    
    @Override
    public TransactionsResponse updateTransactions() {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        PlaidHttpRequest request = new PlaidHttpRequest("/connect", authenticationParams(), timeout);

        HttpResponseWrapper<TransactionsResponse> response =
                httpDelegate.doGet(request, TransactionsResponse.class);

        TransactionsResponse body = response.getResponseBody();
        setAccessToken(body.getAccessToken());
        return body;

    }
    
    @Override
    public TransactionsResponse updateTransactions(GetOptions options) {
    	if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }
        
        Map<String, Object> requestParams = new HashMap<String, Object>();
        if (options != null) {
        	requestParams.put("options", options);
        }

        return handlePost("/connect/get", requestParams, TransactionsResponse.class);
    }
    
    @Override
    public AccountsResponse updateAuth() {
    	if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }
        
        Map<String, Object> requestParams = new HashMap<String, Object>();

        return handlePost("/auth/get", requestParams, AccountsResponse.class);
    }

    @Override
    public TransactionsResponse updateCredentials(Credentials credentials, String type) {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        PlaidHttpRequest request = new PlaidHttpRequest("/connect", authenticationParams(), timeout);

        try {
            String credentialsString = jsonMapper.writeValueAsString(credentials);
            request.addParameter("credentials", credentialsString);            
            request.addParameter("type", type);

            HttpResponseWrapper<TransactionsResponse> response =
                    httpDelegate.doPatch(request, TransactionsResponse.class);

            TransactionsResponse body = response.getResponseBody();

            setAccessToken(body.getAccessToken());

            return body;
        }
        catch (JsonProcessingException e) {
            throw new PlaidClientsideException(e);
        }

    }

    @Override
    public MessageResponse deleteUser() {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        PlaidHttpRequest request = new PlaidHttpRequest("/connect", authenticationParams(), timeout);

        HttpResponseWrapper<MessageResponse> response =
                httpDelegate.doDelete(request, MessageResponse.class);

        return response.getResponseBody();
    }

    @Override
    public AccountsResponse checkBalance() {

    	if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

    	Map<String, Object> requestParams = new HashMap<String, Object>();
    	
    	return handlePost("/balance", requestParams, AccountsResponse.class);
    }

    @Override
    public TransactionsResponse addProduct(String product, ConnectOptions options) {
    	
    	if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

    	Map<String, Object> requestParams = new HashMap<String, Object>();
    	requestParams.put("upgrade_to", product);
    	
    	requestParams.put("login",true);
    	
    	if (options != null) {
    		requestParams.put("options", options);
    	}
    	
    	return handlePost("/upgrade", requestParams, TransactionsResponse.class);
    }
    
    @Override
    public InfoResponse info(Credentials credentials, String type, InfoOptions options) {
    	 Map<String, Object> requestParams = new HashMap<String, Object>();
         requestParams.put("credentials", credentials);
         requestParams.put("type", type);
         requestParams.put("options", options);

         return handlePost("/info", requestParams, InfoResponse.class);
    }
    
    private <T extends PlaidUserResponse> T handleMfa(String path, String mfa, String type, Class<T> returnTypeClass) throws PlaidMfaException {

        if (StringUtils.isEmpty(accessToken)) {
            throw new PlaidClientsideException("No accessToken set");
        }

        Map<String, Object> requestParams = new HashMap<String, Object>();

        requestParams.put("mfa", mfa);

        if (type != null) {
            requestParams.put("type", type);
        }

        return handlePost(path, requestParams, returnTypeClass);
    }

    private <T extends PlaidUserResponse> T handlePost(String path, Map<String, Object> requestParams, Class<T> returnTypeClass) throws PlaidMfaException {

        PlaidHttpRequest request = new PlaidHttpRequest(path, authenticationParams(), timeout);

        try {
            for (String param : requestParams.keySet()) {
                Object value = requestParams.get(param);

                if (value == null) {
                    continue;
                }

                String stringValue;
                if (value instanceof String) {
                    stringValue = (String) value; // strings can be used as is

                } else {
                    stringValue = jsonMapper.writeValueAsString(value); // other objects need to be serialized
                }

                request.addParameter(param, stringValue);
            }

        }
        catch (JsonProcessingException e) {
            throw new PlaidClientsideException(e);
        }

        try {
            HttpResponseWrapper<T> response = httpDelegate.doPost(request, returnTypeClass);

            T body = response.getResponseBody();
            setAccessToken(body.getAccessToken());
            return body;

        }
        catch (PlaidMfaException e) {
            setAccessToken(e.getMfaResponse().getAccessToken());
            throw e;
        }
    }

    private Map<String, String> authenticationParams() {

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("client_id", clientId);
        parameters.put("secret", secret);
        if (!StringUtils.isEmpty(accessToken)) {
            parameters.put("access_token", accessToken);
        }
        return parameters;
    }

    @Override
    public HttpDelegate getHttpDelegate() {
    	return httpDelegate;
    }

    public static class Builder {
        private String clientId;
        private String secret;
        private Integer timeout;
        private HttpDelegate httpDelegate;

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder withTimeout(Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withHttpDelegate(HttpDelegate httpDelegate) {
            this.httpDelegate = httpDelegate;
            return this;
        }

        public DefaultPlaidUserClient build() {
            DefaultPlaidUserClient client = new DefaultPlaidUserClient();
            client.clientId = this.clientId;
            client.secret = this.secret;
            client.timeout = this.timeout;
            client.httpDelegate = this.httpDelegate;

            return client;
        }

    }
}
