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
      "keySize": 2048,
      "templateOid": "1.3.6.1.4.1.311.21.8.3295615.9391522.3558334.2790417.1961463.187.10509973.13081228",
      "major": 100,
      "minor": 26,
}
```

### Valid Example

```json
{
      "keySize": 2048,
      "templateOid": "1.3.6.1.4.1.311.21.8.3295615.9391522.3558334.2790417.1961463.187.10509973.13081228",
      "major": 100,
      "minor": 26,
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
| en name | name (plain text) | "anyName"
| serial number | serial number (plain text) | "123"
| config-file  | Path to JSON config file                              | `/home/orgs/sdk/config.json`   |

**Output**

| File Name   | Description                              | Location             |
|-------------|------------------------------------------|----------------------|
| {enName}{creationTime}.csr | Encrypted CSR (Base64)                  | Output directory     |
| {enName}{creationTime}.key | Encrypted private key                   | Output directory     |
| {enName}{creationTime}.pub  | Encrypted public key                    | Output
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
|{enName}{creationTime}.csr | Encrypted CSR (Base64)                  | Output directory     |
| {enName}{creationTime}.key | Encrypted private key                   | Output directory     |
| {enName}{creationTime}.pub  | Encrypted public key
| einvoice_test_file.xml | Sample E-Invoice for testing               | Output directory  |
| production_csid.cert   | Encrypted production certificate           | Output directory  |
| production_response.json| Encrypted response from Fotara            | Output directory  |

---

### `invoice-validate`

**Args**

| Arg Name                | Description                               | Example                       |
|-------------------------|-------------------------------------------|-------------------------------|
| einvoice-xml-file-path  | Path to E-Invoice XML file (UBL 2.1)      | `/home/orgs/sdk/invoice.xml`  |

**Output (Console)**

- XSD VALIDATION
- CALCULATION VALIDATION
- REGULATION VALIDATION

---

### `invoice-sign`

**Args**

| Arg Name         | Description                                      | Example                              |
|------------------|--------------------------------------------------|--------------------------------------|
| xml-path         | Path to E-Invoice XML (UBL 2.1)                  | `/home/orgs/sdk/invoice.xml`         |
| private-key-path | Encrypted private key path                       | `/home/orgs/sdk/output/{enName}{creationTime}.key`  |
| certificate-path | Encrypted certificate path                       | `/home/orgs/sdk/output/production_csid.cer` |
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
| private-key-path | Encrypted private key path                       | `/home/orgs/sdk/output/{enName}{creationTime}.pem`  |
| certificate-path | Encrypted certificate path                       | `/home/orgs/sdk/output/production_csid.cer` |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Invoice Hash   | Hash of the invoice              | Console/Log  |
| QR Code        | Generated QR code                | Console/Log  |

---

### `submit-clearance`

**Args**

| Arg Name                             | Description                             | Example                                    |
|--------------------------------------|-----------------------------------------|--------------------------------------------|
| client-id                      | the clients identification number        | "321"
| secret-key | the clients password | "123" |
| signed-xml-path                          | path of the signed invoice   | `/home/orgs/sdk/signed-invoice.xml`     |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Response code      | Console response from QPT     | Console/Log  |
| Response body  | A successful invoice submission response indicating the invoice was already submitted previously, with validation passed and QR code generated.    | Console/Log  |

---




### `submit-report`

**Args**

| Arg Name                             | Description                             | Example                                    |
|--------------------------------------|-----------------------------------------|--------------------------------------------|
| client-id                      | the clients identification number        | "321"
| secret-key | the clients password | "123" |
| signed-xml-path                          | path of the signed invoice   | `/home/orgs/sdk/signed-invoice.xml     |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Response code      | Console response from QPT     | Console/Log  |
| Response body  | A successful invoice submission with warnings if any exist    | Console/Log  |



---

### `compliance-invoice`

**Args**

| Arg Name                             | Description                             | Example                                    |
|--------------------------------------|-----------------------------------------|--------------------------------------------|
| signed-xml-path                     | path of signed xml        | `/home/orgs/sdk/signed-invoice.xml`
| compliance-certificate-path | path of compliance certificate | `/home/orgs/sdk/test.cer` |
| output-path                         | outpath of the certificate   | `/home/orgs/out/eInvoiceResponse`     |

**Output**

| Output         | Description                     | Location     |
|----------------|----------------------------------|--------------|
| Response       | E-invoice response in Json     | Console/Log  |
| output  | the invoice    | output path  |

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
