/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: paulo.abrantes@qub-it.com
 *
 * 
 * This file is part of FenixEdu bennu-webservices.
 *
 * FenixEdu bennu-webservices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu bennu-webservices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu bennu-webservices.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.qubit.solution.fenixedu.bennu.webservices.services.client;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.net.util.Base64;

import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceClientConfiguration;

public class WebServiceClientHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String AUTH_NS = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    private static final String AUTH_PREFIX = "wss";
    private WebServiceClientConfiguration clientConfiguration;
    private String username;
    private String password;

    public WebServiceClientHandler(WebServiceClientConfiguration clientConfiguration) {
    }

    public WebServiceClientHandler(WebServiceClientConfiguration webServiceClientConfiguration, String username, String password) {
        this.clientConfiguration = webServiceClientConfiguration;
        this.username = username;
        this.password = password;
    }

    public boolean handleMessage(SOAPMessageContext smc) {

        boolean isOutbound = (Boolean) smc.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (isOutbound) {
            try {

                final byte[] sessionKey = generateAESKey();
                final String encriptedPassword = cypher(sessionKey, password);
                final String encriptedTimestamp = cypher(sessionKey, getTimestamp());
                final String nonce = cypherSessionKey(getPublicKey(), sessionKey);

                SOAPEnvelope envelope = smc.getMessage().getSOAPPart().getEnvelope();
                SOAPFactory soapFactory = SOAPFactory.newInstance();

                // WSSecurity <Security> header
                SOAPElement wsSecHeaderElm = soapFactory.createElement("Security", AUTH_PREFIX, AUTH_NS);
                SOAPElement userNameTokenElm = soapFactory.createElement("UsernameToken", AUTH_PREFIX, AUTH_NS);
                // Username
                SOAPElement userNameElm = soapFactory.createElement("Username", AUTH_PREFIX, AUTH_NS);
                userNameElm.addTextNode(username);
                // Password
                SOAPElement passwdElm = soapFactory.createElement("Password", AUTH_PREFIX, AUTH_NS);
                passwdElm.addTextNode(encriptedPassword);
                // Nonce
                SOAPElement nonceElm = soapFactory.createElement("Nonce", AUTH_PREFIX, AUTH_NS);
                nonceElm.addTextNode(nonce);
                // Created
                SOAPElement createdElm = soapFactory.createElement("Created", AUTH_PREFIX, AUTH_NS);
                createdElm.addTextNode(encriptedTimestamp);

                userNameTokenElm.addChildElement(userNameElm);
                userNameTokenElm.addChildElement(passwdElm);
                userNameTokenElm.addChildElement(nonceElm);
                userNameTokenElm.addChildElement(createdElm);

                // add child elements to the root element
                wsSecHeaderElm.addChildElement(userNameTokenElm);

                SOAPHeader sh = envelope.getHeader();
                if (sh == null) {
                    // create SOAPHeader instance for SOAP envelope
                    sh = envelope.addHeader();
                }

                // add SOAP element for header to SOAP header object
                sh.addChildElement(wsSecHeaderElm);

            } catch (Exception e) {
                throw new RuntimeException("Problems in the securityHandler", e);
            }
        }
        return true;
    }

    private Key getPublicKey() {
        return this.clientConfiguration.getDomainKeyStore().getHelper()
                .getCertificate(this.clientConfiguration.getAliasForCerficate()).getPublicKey();
    }

    public static String cypher(byte[] sessionKey, String informationToCipher) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKey, "AES"));
            byte[] cipherData = cipher.doFinal(informationToCipher.getBytes("UTF-8"));
            return Base64.encodeBase64String(cipherData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String cypherSessionKey(Key publicKey, byte[] simetricKey) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeBase64String(cipher.doFinal(simetricKey));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------
    // Utility Methods
    // ---------------

    private static byte[] generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            return generator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SimpleDateFormat TIMESTAMP_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    static {
        TIMESTAMP_FORMATER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static String getTimestamp() throws ParseException {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date time = c.getTime();
        return TIMESTAMP_FORMATER.format(time);
    }

    public void close(MessageContext arg0) {

    }

    public boolean handleFault(SOAPMessageContext arg0) {
        return false;
    }

    public Set<QName> getHeaders() {
        return null;
    }
}
