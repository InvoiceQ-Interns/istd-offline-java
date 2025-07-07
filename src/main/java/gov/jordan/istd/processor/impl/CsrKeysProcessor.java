package gov.jordan.istd.processor.impl;

import gov.jordan.istd.dto.CsrConfigDto;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.utils.ECDSAUtil;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.microsoft.MicrosoftObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Objects;

public class CsrKeysProcessor extends ActionProcessor {

    private String outputDirectory="";
    private String configFilePath="";
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private CsrConfigDto csrConfigDto;
    private String csrEncoded;
    private String privateKeyPEM;
    private String publicKeyPEM;
    private String csrPem;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 2) {
            log.info("Usage: java -jar fotara-sdk.jar generate-csr-keys <directory> <config-file>");
            return false;
        }
        outputDirectory = args[0];
        configFilePath = args[1];
        return true;
    }

    @Override
    protected boolean validateArgs() {
        if (!ReaderHelper.isDirectoryExists(outputDirectory)) {
            log.info(String.format("Output directory [%s] does not exist", outputDirectory));
            return false;
        }
        if (StringUtils.isBlank(configFilePath)) {
            log.info(String.format("Config file [%s] does not exist", configFilePath));
            return false;
        }
        String configFile = ReaderHelper.readFileAsString(configFilePath);
        if(StringUtils.isBlank(configFile)){
            log.info(String.format("Config file [%s] is empty", configFilePath));
            return false;
        }
        csrConfigDto= JsonUtils.readJson(configFile,CsrConfigDto.class);
        if(Objects.isNull(csrConfigDto)){
            log.info(String.format("Config file [%s] is invalid", configFilePath));
            return false;
        }
        boolean isValid=true;
        if(StringUtils.isBlank(csrConfigDto.getCommonName())){
            log.info(String.format("Common name is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getSerialNumber())){
            log.info(String.format("Serial number is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getOrganizationIdentifier())){
            log.info(String.format("Organization identifier is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getOrganizationUnitName())){
            log.info(String.format("Organization unit name is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getOrganizationName())){
            log.info(String.format("Organization name is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getCountryName())){
            log.info(String.format("Country name is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getInvoiceType())){
            log.info(String.format("Invoice type is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getLocation())){
            log.info(String.format("Location is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(StringUtils.isBlank(csrConfigDto.getIndustry())){
            log.info(String.format("Industry is missing in config file [%s]", configFilePath));
            isValid=false;
        }
        if(isValid && StringUtils.split(csrConfigDto.getSerialNumber(),"|").length!=3){
            log.info(String.format("Serial number [%s] is invalid, format [TAX_NUMBER|SEQ_NUMBER|DEVICE_ID]", csrConfigDto.getSerialNumber()));
            isValid = false;
        }
        if(isValid && (csrConfigDto.getInvoiceType().length()!=4 || !csrConfigDto.getInvoiceType().matches("[01]+"))){
            log.info(String.format("Invoice type [%s] is invalid, format [4-digit-number (0/1)]", csrConfigDto.getInvoiceType()));
            isValid=false;
        }
        return isValid;
    }

    @Override
    protected boolean process() {
        if (!generateKeyPairs() || Objects.isNull(publicKey) || Objects.isNull(privateKey)){
            log.error("Failed to generate CSR keys");
            return false;
        }
        if(!buildCsr() || StringUtils.isBlank(csrEncoded) ){
            log.error("Failed to build CSR");
            return false;
        }
        privateKeyPEM = transform("EC PRIVATE KEY", privateKey.getEncoded());
        publicKeyPEM = transform("EC PUBLIC KEY", publicKey.getEncoded());
        return true;
    }
    @Override
    protected boolean output() {
        log.info(String.format("CSR [%s]",SecurityUtils.decrypt(csrEncoded)));
        String privateKeyFile = outputDirectory + "/private.pem";
        String publicKeyFile = outputDirectory + "/public.pem";
        String csrFile = outputDirectory + "/csr.pem";
        String csrEncodedFile = outputDirectory + "/csr.encoded";
        boolean valid= WriterHelper.writeFile(privateKeyFile, SecurityUtils.encrypt(privateKeyPEM));
        valid= WriterHelper.writeFile(publicKeyFile,SecurityUtils.encrypt(publicKeyPEM)) && valid;
        valid= WriterHelper.writeFile(csrFile,SecurityUtils.encrypt(csrPem)) && valid;
        valid= WriterHelper.writeFile(csrEncodedFile,SecurityUtils.encrypt(csrEncoded)) && valid;
        return valid;
    }


    private boolean buildCsr() {
        try {
            String certificateTemplateName=propertiesManager.getProperty("fotara.certificate.template");
            X500Name x500 = buildX500SubjectBlock();
            X500Name x500OtherAttributes = buildX500AttributesBlock();
            Extension subjectAltName = new Extension(MicrosoftObjectIdentifiers.microsoftCertTemplateV1, false,
                    new DEROctetString(new DisplayText(2, certificateTemplateName)));
            GeneralName[] generalNamesArray = {new GeneralName(x500OtherAttributes)};
            GeneralNames generalNames = new GeneralNames(generalNamesArray);
            ContentSigner signGen = (new JcaContentSignerBuilder("SHA256WITHECDSA")).build(privateKey);
            JcaPKCS10CertificationRequestBuilder jcaPKCS10CertificationRequestBuilder = new JcaPKCS10CertificationRequestBuilder(x500, publicKey);
            jcaPKCS10CertificationRequestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                            new Extensions(new Extension[]{subjectAltName, Extension.create(Extension.subjectAlternativeName, false, generalNames)}))
                    .build(signGen);
            PKCS10CertificationRequest certRequest = jcaPKCS10CertificationRequestBuilder.build(signGen);
            csrPem = transform("CERTIFICATE REQUEST", certRequest.getEncoded());
            assert csrPem != null;
            csrEncoded = new String(Base64.getEncoder().encode(csrPem.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }catch (Exception e){
            log.error("Failed to build CSR ",e);
            return false;
        }
        return true;

    }
    private X500Name buildX500SubjectBlock() {
        final X500NameBuilder subject = new X500NameBuilder();
        subject.addRDN(BCStyle.C, csrConfigDto.getCountryName());
        subject.addRDN(BCStyle.OU, csrConfigDto.getOrganizationUnitName());
        subject.addRDN(BCStyle.O, csrConfigDto.getOrganizationName());
        subject.addRDN(BCStyle.CN, csrConfigDto.getCommonName());
        return subject.build();
    }

    private X500Name buildX500AttributesBlock() {
        final X500NameBuilder x500NameBuilderOtherAttributes = new X500NameBuilder();
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.sn, csrConfigDto.getSerialNumber());
        x500NameBuilderOtherAttributes.addRDN(BCStyle.UID, csrConfigDto.getOrganizationIdentifier());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.title, csrConfigDto.getInvoiceType());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.registeredAddress, csrConfigDto.getLocation());
        x500NameBuilderOtherAttributes.addRDN(RFC4519Style.businessCategory, csrConfigDto.getIndustry());
        return x500NameBuilderOtherAttributes.build();
    }

    private String transform(final String type, final byte[] certificateRequest) {
        try {
            final PemObject pemObject = new PemObject(type, certificateRequest);
            final StringWriter stringWriter = new StringWriter();
            final PEMWriter pemWriter = new PEMWriter(stringWriter);
            pemWriter.writeObject(pemObject);
            pemWriter.close();
            stringWriter.close();
            return stringWriter.toString();
        } catch (Exception e) {
            log.error("something went wrong", e);
            return null;
        }
    }
    private boolean generateKeyPairs() {
        try {
            KeyPair pair = ECDSAUtil.getKeyPair();
            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();
        }catch (Exception e){
            log.error("Failed to generate CSR keys",e);
            return false;
        }
        return true;
    }

}
