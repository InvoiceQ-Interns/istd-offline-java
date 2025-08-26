# Fotara SDK

## About The SDK

This SDK is a Java-based library packaged as a JAR file, designed to help taxpayers seamlessly integrate with the national e-invoicing system. It provides end-to-end support for secure invoice generation and submission, including:

- Certificate Signing Request (CSR) creation
- Private key management
- QR code generation
- Digital signing
- Secure submission via the reporting API

By automating compliance with e-invoicing regulations, the SDK ensures data integrity, authenticity, and a streamlined integration experience for developers.

---

## Prerequisites

- Java 11–17 installed on your device.
- Permission to read/write from the target storage.
- Active account on Fotara.
- Valid taxpayer config file.

---

## Taxpayer Config File

```json
{
    "commonName": "", // Corporate Name
    "serialNumber": "", // Format: "TAXPAYER_NUMBER|SEQUENCE_NUMBER|DEVICE_ID"
    "organizationIdentifier": "", // Main CNAT number
    "organizationUnitName": "", // Child Corporate Name if exists
    "organizationName": "", // District
    "countryName": "JO", // ISO-2 code, should be "JO"
    "invoiceType": "1100", // 4-digit code: B2B, B2C, Special Tax, Export
    "location": "", // Main branch location
    "industry": "", // Sector (free text)
    "email": "" // email
}
```

### Valid Example

```json
{
  "commonName": "Test Corps",
  "serialNumber": "12342131312|1242412412|53222122",
  "organizationIdentifier": "125252125",
  "organizationUnitName": "Test Child Corp",
  "organizationName": "Amman",
  "countryName": "JO",
  "invoiceType": "1100",
  "location": "Amman, Jordan",
  "industry": "Retail",
  "email": "email@email.com"
}
```

---

## How To Use

Use the SDK from the command line in the following format:

```bash
java -Denv=<env> -jar fotara-sdk-1.0.6-jar-with-dependencies.jar <action> <args>
```

### Environments

- `dev`: For development/testing
- `sim`: Fotara simulation environment
- `prod`: Fotara production environment

---

## Actions

### `generate-csr-keys`

**Args**

| Arg Name     | Description                                           | Example                        |
|--------------|-------------------------------------------------------|--------------------------------|
| directory    | Output path for generated files (encrypted)           | `/home/orgs/sdk/output`        |
| config-file  | Path to JSON config file                              | `/home/orgs/sdk/config.json`   |

**Output**

| File Name   | Description                              | Location             |
|-------------|------------------------------------------|----------------------|
| csr.encoded | Encrypted CSR (Base64)                  | Output directory     |
| private.pem | Encrypted private key                   | Output directory     |
| public.pem  | Encrypted public key                    | Output directory     |
| csr.pem     | Encrypted CSR in PEM format             | Output directory     |

---

### `onboard`

**Args**

| Arg Name     | Description                                           | Example                        |
|--------------|-------------------------------------------------------|--------------------------------|
| otp          | OTP from Fotara portal                                | `123111`                       |
| directory    | Output path for generated files (encrypted)           | `/home/orgs/sdk/output`        |
| config-file  | Path to JSON config file                              | `/home/orgs/sdk/config.json`   |

**Output**

| File Name              | Description                                 | Location          |
|------------------------|---------------------------------------------|-------------------|
| csr.encoded            | Encrypted CSR (Base64)                      | Output directory  |
| private.pem            | Encrypted private key                      | Output directory  |
| public.pem             | Encrypted public key                       | Output directory  |
| csr.pem                | Encrypted CSR in PEM format                | Output directory  |
| einvoice_test_file.xml | Sample E-Invoice for testing               | Output directory  |
| production_csid.cert   | Encrypted production certificate           | Output directory  |
| production_response.json| Encrypted response from Fotara            | Output directory  |

---

### `validate`

**Args**

| Arg Name                | Description                               | Example                       |
|-------------------------|-------------------------------------------|-------------------------------|
| einvoice-xml-file-path  | Path to E-Invoice XML file (UBL 2.1)      | `/home/orgs/sdk/invoice.xml`  |

**Output (Console)**

- XSD VALIDATION
- CALCULATION VALIDATION
- REGULATION VALIDATION

---

### `sign`

**Args**

| Arg Name         | Description                                      | Example                              |
|------------------|--------------------------------------------------|--------------------------------------|
| xml-path         | Path to E-Invoice XML (UBL 2.1)                  | `/home/orgs/sdk/invoice.xml`         |
| private-key-path | Encrypted private key path                       | `/home/orgs/sdk/output/private.pem`  |
| certificate-path | Encrypted certificate path                       | `/home/orgs/sdk/output/production_csid.cert` |
| output-path      | Output path for signed XML                       | `/home/orgs/sdk/output/signed_invoice.xml` |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Invoice Hash   | Hash of the invoice              | Console/Log  |
| QR Code        | Generated QR code                | Console/Log  |
| Signed XML     | Final signed invoice XML         | Output path  |

---

### `generate-qr`

**Args**

| Arg Name         | Description                                      | Example                              |
|------------------|--------------------------------------------------|--------------------------------------|
| xml-path         | Path to E-Invoice XML (UBL 2.1)                  | `/home/orgs/sdk/invoice.xml`         |
| private-key-path | Encrypted private key path                       | `/home/orgs/sdk/output/private.pem`  |
| certificate-path | Encrypted certificate path                       | `/home/orgs/sdk/output/production_csid.cert` |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Invoice Hash   | Hash of the invoice              | Console/Log  |
| QR Code        | Generated QR code                | Console/Log  |

---

### `submit`

**Args**

| Arg Name                             | Description                             | Example                                    |
|--------------------------------------|-----------------------------------------|--------------------------------------------|
| signed-xml-path                      | Path to signed XML                      | `/home/orgs/sdk/signed_invoice.xml`        |
| production-certificate-response-path| Encrypted response file from onboarding | `/home/orgs/sdk/output/production_response.json` |
| output-path                          | Path to write the submission response   | `/home/orgs/sdk/output/submit_response.json`     |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Response       | Console response from Fotara     | Console/Log  |
| file response  | JSON response written to file    | Output path  |

---

### `decrypt`

**Args**

| Arg Name             | Description                             | Example                            |
|----------------------|-----------------------------------------|------------------------------------|
| encrypted-file-path  | Encrypted file to be decrypted          | `/home/orgs/sdk/private.pem`       |

**Output**

| Output         | Description                             | Location     |
|----------------|------------------------------------------|--------------|
| decrypted file | Plain text output of the file            | Console/Log  |
