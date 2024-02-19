package de.medizininformatik_initiative.process.feasibility.tools.generator;

import de.medizininformatik_initiative.process.feasibility.tools.generator.CertificateGenerator.CertificateFiles;
import de.rwh.utils.crypto.CertificateAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class TestDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

    private static final CertificateGenerator certificateGenerator = new CertificateGenerator();
    private static final BundleGenerator bundleGenerator = new BundleGenerator();
    private static final EnvGenerator envGenerator = new EnvGenerator();

    static {
        CertificateAuthority.registerBouncyCastleProvider();
    }

    public static void main(String[] args) {
        certificateGenerator.generateCertificates();
        certificateGenerator.copyDockerTestClientCerts();
        certificateGenerator.copyDockerTestServerCert();

        Map<String, CertificateFiles> clientCertificateFilesByCommonName = certificateGenerator.getClientCertificateFilesByCommonName();

        CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");
        Path p12File = certificateGenerator.createP12(webbrowserTestUser);
        logger.warn("Install client-certificate and CA certificate from \"{}\" into your browsers certificate store to access fhir and bpe servers with your webbrowser", p12File.toAbsolutePath());

        bundleGenerator.createDockerTestBundles(clientCertificateFilesByCommonName);
        bundleGenerator.copyDockerTestBundles();

        envGenerator.generateAndWriteDockerTestFhirEnvFiles(clientCertificateFilesByCommonName);
    }
}
