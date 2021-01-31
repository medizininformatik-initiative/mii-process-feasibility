package de.netzwerk_universitaetsmedizin.codex.processes.tools.generator;

import de.netzwerk_universitaetsmedizin.codex.processes.tools.generator.CertificateGenerator.CertificateFiles;
import de.rwh.utils.crypto.CertificateAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TestDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

    private static final CertificateGenerator certificateGenerator = new CertificateGenerator();
    private static final BundleGenerator bundleGenerator = new BundleGenerator();
    private static final ConfigGenerator configGenerator = new ConfigGenerator();

    static {
        CertificateAuthority.registerBouncyCastleProvider();
    }

    public static void main(String[] args) {
        certificateGenerator.generateCertificates();
        certificateGenerator.copyDockerTestCertificates();

        Map<String, CertificateFiles> certificateFilesByCommonName = certificateGenerator.getClientCertificateFilesByCommonName();

        CertificateFiles webbrowserTestUser = certificateFilesByCommonName.get("Webbrowser Test User");
        logger.warn("Install client-certificate and CA certificate from \"{}\" into your browsers certificate store to access fhir and bpe servers with your webbrowser", webbrowserTestUser.getP12KeyStoreFile().toAbsolutePath().toString());

        bundleGenerator.createDockerTestBundles(certificateFilesByCommonName);
        bundleGenerator.copyDockerTestBundles();

        configGenerator.modifyDockerTestFhirConfigProperties(certificateFilesByCommonName);
        configGenerator.copyDockerTestFhirConfigProperties();
    }
}
