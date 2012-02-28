package org.apache.airavata.migrator.registry;

import org.apache.airavata.common.registry.api.exception.RegistryException;
import org.apache.airavata.commons.gfac.type.ApplicationDeploymentDescription;
import org.apache.airavata.commons.gfac.type.HostDescription;
import org.apache.airavata.commons.gfac.type.ServiceDescription;
import org.apache.airavata.registry.api.impl.AiravataJCRRegistry;
import org.apache.xmlbeans.XmlException;
import org.ogce.schemas.gfac.beans.ApplicationBean;
import org.ogce.schemas.gfac.beans.HostBean;
import org.ogce.schemas.gfac.beans.ServiceBean;
import org.ogce.xregistry.client.XRegistryClient;
import org.ogce.xregistry.client.XRegistryClientUtil;
import org.ogce.xregistry.utils.XRegistryClientException;
import xregistry.generated.FindAppDescResponseDocument;
import xregistry.generated.HostDescData;
import xregistry.generated.ServiceDescData;

import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class XRegistryMigrate {
//    private static String propertyfile = "xregistry.properties";
//    private static String propertyfile = "xregistry-dropbox.properties";
    private static String propertyfile = "xregistry-local.properties";
    private static AiravataJCRRegistry jcrRegistry = null;

    private static final String JCR_URL = "jcr.url";
    private static final String JCR_USERNAME = "jcr.username";
    private static final String JCR_PASSWORD = "jcr.password";

    public static void main(String[] args) throws XRegistryClientException {
        Properties prop = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propertyfile);
            prop.load(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        String jcrRegsitryURL = prop.getProperty(JCR_URL);
        String jcrUsername = prop.getProperty(JCR_USERNAME);
        String jcrPassword = prop.getProperty(JCR_PASSWORD);

        /* Create database */
        Map<String,String> config = new HashMap<String,String>();
        config.put("org.apache.jackrabbit.repository.home","target");
        try {
            jcrRegistry = new AiravataJCRRegistry(new URI(jcrRegsitryURL),
                    "org.apache.jackrabbit.core.RepositoryFactoryImpl",
                    jcrUsername, jcrPassword, config);
        } catch (RepositoryException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        XRegistryClient client = XRegistryClientUtil.CreateGSISecureRegistryInstance(propertyfile);
        saveAllHostDescriptions(client);
        saveAllServiceDescriptions(client);

        System.out.println("DONE!");
    }

    private static void saveAllHostDescriptions(XRegistryClient client) throws XRegistryClientException {
        HostDescription host = null;
        HostDescData[] hostDescs = client.findHosts("");
        Map<QName, HostDescData> val = new HashMap<QName, HostDescData>();
        for (HostDescData hostDesc : hostDescs) {
            val.put(hostDesc.getName(), hostDesc);
            String hostDescStr = client.getHostDesc(hostDesc.getName().getLocalPart());
            HostBean hostBean = null;
            try {
                hostBean = org.ogce.schemas.gfac.beans.utils.HostUtils.simpleHostBeanRequest(hostDescStr);
                System.out.println("Host : " + hostBean.getHostName());
                System.out.println(hostDescStr);

            } catch (XmlException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if(hostBean != null){
                host = MigrationUtil.createHostDescription(hostBean);
            }

            try {
                jcrRegistry.saveHostDescription(host);
            } catch (RegistryException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }

        System.out.println("=== All Hosts are saved ===");
    }

    private static void saveAllServiceDescriptions(XRegistryClient client) throws XRegistryClientException {
        ServiceDescription service = null;
        ServiceDescData[] serviceDescDatas = client.findServiceDesc("");
        Map<QName, ServiceDescData> val3 = new HashMap<QName, ServiceDescData>();

        for (ServiceDescData serviceDesc : serviceDescDatas) {
            val3.put(serviceDesc.getName(), serviceDesc);
            String serviceDescStr = client.getServiceDesc(serviceDesc.getName());
            ServiceBean serviceBean = null;
            String applicationName = null;

            try {
                serviceBean = org.ogce.schemas.gfac.beans.utils.ServiceUtils.serviceBeanRequest(serviceDescStr);
                applicationName = serviceBean.getApplicationName();
                System.out.println("Service : " + serviceBean.getServiceName());
                System.out.println(serviceDescStr);
            } catch (XmlException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if(serviceBean != null) {
                service = MigrationUtil.createServiceDescription(serviceBean);
                try {
                    jcrRegistry.saveServiceDescription(service);
                    ApplicationBean appBean = saveApplicationDescriptionWithName(client, applicationName, service);
                    // TODO : should look into this
                    if (appBean != null){
                        jcrRegistry.deployServiceOnHost(service.getType().getName(), appBean.getHostName());
                    }
                } catch (RegistryException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

        }
        System.out.println("=== All Service/Applciation descriptors are saved ===");
    }

    private static ApplicationBean saveApplicationDescriptionWithName(XRegistryClient client, String applicationName, ServiceDescription service) throws XRegistryClientException {
        ApplicationDeploymentDescription app = null;
        FindAppDescResponseDocument.FindAppDescResponse.AppData[] appDatas = client.findAppDesc(applicationName);
        Map<QName, FindAppDescResponseDocument.FindAppDescResponse.AppData> val2 =
                new HashMap<QName, FindAppDescResponseDocument.FindAppDescResponse.AppData>();
        ApplicationBean appBean = null;
        for (FindAppDescResponseDocument.FindAppDescResponse.AppData appDesc : appDatas) {
            val2.put(appDesc.getName(), appDesc);
            String appDescStr = client.getAppDesc(appDesc.getName().toString(),appDesc.getHostName());
            try {
                appBean = org.ogce.schemas.gfac.beans.utils.ApplicationUtils.simpleApplicationBeanRequest(appDescStr);
                System.out.println("Application : " + appBean.getApplicationName());
                System.out.println(appDescStr);

            } catch (XmlException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if(appBean != null){
                app = MigrationUtil.createAppDeploymentDescription(appBean);
                try {
                    jcrRegistry.saveDeploymentDescription(service.getType().getName(), appBean.getHostName(), app);
//            jcrRegistry.saveDeploymentDescription(service.getType().getName(), host.getType().getHostName(), app);
                } catch (RegistryException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }


        }

        return appBean;
    }

    private static void saveAllApplicationDescriptions(XRegistryClient client, ServiceDescription service) throws XRegistryClientException {
        ApplicationDeploymentDescription app = null;
        FindAppDescResponseDocument.FindAppDescResponse.AppData[] appDatas = client.findAppDesc("");
        Map<QName, FindAppDescResponseDocument.FindAppDescResponse.AppData> val2 =
                new HashMap<QName, FindAppDescResponseDocument.FindAppDescResponse.AppData>();
        for (FindAppDescResponseDocument.FindAppDescResponse.AppData appDesc : appDatas) {
            val2.put(appDesc.getName(), appDesc);
            String appDescStr = client.getAppDesc(appDesc.getName().toString(),appDesc.getHostName());
            System.out.println(appDescStr);
            ApplicationBean appBean = null;
            try {
                appBean = org.ogce.schemas.gfac.beans.utils.ApplicationUtils.simpleApplicationBeanRequest(appDescStr);
            } catch (XmlException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if(appBean != null){
                app = MigrationUtil.createAppDeploymentDescription(appBean);
            }

            //jcrRegistry.saveDeploymentDescription(service.getType().getName(), host.getType().getHostName(), app);

        }
    }

}

