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

package com.qubit.solution.fenixedu.bennu.webservices.domain.webservice;

import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.Bennu;

import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;

public class WebServiceClientConfiguration extends WebServiceClientConfiguration_Base {

    protected WebServiceClientConfiguration() {
        super();
    }

    public WebServiceClientConfiguration(String implementationClass) {
        this();
        setImplementationClass(implementationClass);
    }

    public boolean isSecured() {
        return getAuthenticationLevel() != null && getAuthenticationLevel() != WebServiceAuthenticationLevel.NONE;
    }

    public boolean isSSLActive() {
        return getSslActive();
    }

    public <T extends BennuWebServiceClient> T getClient() {
        try {
            Class<? extends BennuWebServiceClient> clazz =
                    (Class<? extends BennuWebServiceClient>) Class.forName(getImplementationClass());
            return (T) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Collection<WebServiceClientConfiguration> readAll() {
        return Bennu.getInstance().getWebserviceConfigurationsSet().stream()
                .filter(configuration -> configuration.getClass() == WebServiceClientConfiguration.class)
                .map(WebServiceClientConfiguration.class::cast).collect(Collectors.toList());
    }

}
