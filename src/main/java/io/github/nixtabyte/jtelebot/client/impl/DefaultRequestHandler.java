/**
 * 
 * Copyright (C) 2015 Roberto Dominguez Estrada and Juan Carlos Sedano Salas
 *
 * This material is provided "as is", with absolutely no warranty expressed
 * or implied. Any use is at your own risk.
 *
 */
package io.github.nixtabyte.jtelebot.client.impl;

import io.github.nixtabyte.jtelebot.client.HttpClientFactory;
import io.github.nixtabyte.jtelebot.client.HttpProxy;
import io.github.nixtabyte.jtelebot.client.RequestHandler;
import io.github.nixtabyte.jtelebot.mapper.json.MapperHandler;
import io.github.nixtabyte.jtelebot.request.TelegramRequest;
import io.github.nixtabyte.jtelebot.response.json.TelegramResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;

public class DefaultRequestHandler implements RequestHandler {

	// TODO This should be in a CommonConstants class
	private static final String URL_TEMPLATE = "https://api.telegram.org/bot{0}/{1}";

	private HttpClient httpClient;
	private String token;

	public DefaultRequestHandler() {
		httpClient = HttpClientFactory.createHttpClient();
	}

	public DefaultRequestHandler(final String token) {
		this();
		this.token = token;
	}

	@Override
	public TelegramResponse<?> sendRequest(TelegramRequest telegramRequest) {
		TelegramResponse<?> telegramResponse = null;
		final String response = callHttpService(telegramRequest);

		telegramResponse = parseJsonResponse(response, telegramRequest
				.getRequestType().getResultClass());

		return telegramResponse;
	}

	@Override
	public TelegramResponse<?> sendRequest(TelegramRequest telegramRequest,
			HttpProxy proxy) {
		TelegramResponse<?> telegramResponse = null;
		final String response = callHttpService(telegramRequest, proxy);

		telegramResponse = parseJsonResponse(response, telegramRequest
				.getRequestType().getResultClass());

		return telegramResponse;
	}

	private String callHttpService(TelegramRequest telegramRequest) {
		return this.callHttpService(telegramRequest, null);
	}

	private String callHttpService(TelegramRequest telegramRequest,
			HttpProxy proxy) {
		final String url = MessageFormat.format(URL_TEMPLATE, token,
				telegramRequest.getRequestType().getMethodName());

		final HttpPost request = new HttpPost(url);
		if (telegramRequest.getFile() != null) {
			final MultipartEntityBuilder mpeb = MultipartEntityBuilder.create();
			mpeb.addBinaryBody(telegramRequest.getFileType(),
					telegramRequest.getFile());
			for (BasicNameValuePair bnvp : telegramRequest.getParameters()) {
				mpeb.addTextBody(bnvp.getName(), bnvp.getValue());
			}

			// PROXY Usage
			if (proxy != null) {
				HttpHost proxyHost = new HttpHost(proxy.getHost(),
						proxy.getPort(), proxy.getProtocol());
				RequestConfig config = RequestConfig.custom()
						.setProxy(proxyHost).build();
				request.setConfig(config);
			}

			request.setEntity(mpeb.build());
		} else {
			request.setEntity(new UrlEncodedFormEntity(telegramRequest
					.getParameters(), Consts.UTF_8));
		}
		try {
			final HttpResponse response = httpClient.execute(request);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}

			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Request to Telegram failed!");
				/**
				 * TODO: should we throw an exception?
				 */
			}

			return result.toString();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	// TODO This method should be implemented in a ResponseParser class
	private TelegramResponse<?> parseJsonResponse(final String jsonResponse,
			final Class<?> resultTypeClass) {
		try {

			final TelegramResponse<?> telegramResponse = (TelegramResponse<?>) MapperHandler.INSTANCE
					.getObjectMapper().readValue(
							jsonResponse,
							MapperHandler.INSTANCE
									.getObjectMapper()
									.getTypeFactory()
									.constructParametricType(
											TelegramResponse.class,
											resultTypeClass));

			return telegramResponse;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}