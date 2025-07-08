# About The SDK

This SDK is a Java-based library packaged as a JAR file, designed to help taxpayers seamlessly integrate with the national e-invoicing system. It provides end-to-end support for secure invoice generation and submission, including Certificate Signing Request (CSR) creation, private key management, QR code generation, digital signing, and secure submission via the reporting API. By automating compliance with e-invoicing regulations, the SDK ensures data integrity, authenticity, and a streamlined integration experience for developers.

# Prerequisites

- Java 11- 17 installed on your device.
- permission to read and write from the target storage
- active account on fotara.
- valid taxpayer config file.

# Taxpayer Config File

&nbsp;

```json
{
    "commonName": "",// Corporate Name
    "serialNumber": "",// With the following format "TAXPAYER_NUMBER|SEQUENCE_NUMBER|DEVICE_ID"
    "organizationIdentifier": "",// Main CNAT number
    "organizationUnitName": "", //Child Corporate Name if exists
    "organizationName": "", // District
    "countryName": "JO", // Country ISO-2 code, and should be allows JO
    "invoiceType": "1100", // 4 digits EInvoice Type as the following: 
                   // first digit to allow submit B2B E-Invoices
                   // Second Digit to allow submit B2C E-Invoice
                   // Third Digit to allow submit Special Taxes E-Invoice
                   // Fourth Digits to allow submit Export E-Invoice
    "location": "", // Main Branch location
    "industry": "" // Sector (free text)
}

// valid example
{
  "commonName": "Test Corps",
  "serialNumber": "12342131312|1242412412|53222122",
  "organizationIdentifier": "125252125",
  "organizationUnitName": "Test Child Corp",
  "organizationName": "Amman",
  "countryName": "JO",
  "invoiceType": "1100",
  "location": "Amman, Jordan",
  "industry": "Retail"
}

```

&nbsp;

# How To Use?

you can use the sdk directly through command line with the following format

`java -Denv=<env> -jar fotara-sdk.jar <action> <args>`

- ## Environments:

    - "dev": used for development testing only
    - "sim": used to interact with fotara simulation environment
    - "prod": used to interact with fotara production environment.
- ## Actions:

    - ### generate-csr-keys:

        - args:

        - | Arg Name | Value | Example |
                      | --- | --- | --- |
          | directory | Output Directory, to store the generated key pairs and generated csr (notes all data will be stored encrypted) | /home/orgs/sdk/output |
          | config-file | json config file path | /home/orgs/sdk/config.json |

        - output:

        - | output Name | Value | location |
                      | --- | --- | --- |
          | csr.encoded | Certificate Sign Request Encrypted and Base64 Encoded | inside output path |
          | private.pem | Private Key Encrypted file in pem format | inside output path |
          | public.pem | Public Key Encrypted file in pem format | inside output path |
          | csr.pem | Certificate Sign Request Encrypted in pem format | inside output path |

    - ### onboard:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | otp | otp generated from fotara portal (6 digits) | 123111 |
              | directory | Output Directory, to store the generated key pairs and generated csr (notes all data will be stored encrypted) | /home/orgs/sdk/output |
              | config-file | json config file path | /home/orgs/sdk/config.json |

            - output:

            - | output Name | Value | location |
                              | --- | --- | --- |
              | csr.encoded | Certificate Sign Request Encrypted and Base64 Encoded | inside output path |
              | private.pem | Private Key Encrypted file in pem format | inside output path |
              | public.pem | Public Key Encrypted file in pem format | inside output path |
              | csr.pem | Certificate Sign Request Encrypted in pem format | inside output path |
              | einvoice_test_file.xml | E-Invoice Generated for testing process | inside output path |
              | production_csid.cert | Production Certificate Encrypted | inside output path |
              | production_response.json | Production Response From Fotara Encrypted | inside output path |

    - ### validate:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | eninvoice-xml-file-path | E-Invoice XML file Path Compliance with UBL 2.1 | /home/orgs/sdk/invoice.xml |

            - output:

                - | output Name | Value | location |
                                      | --- | --- | --- |
                  | XSD VALIDATION | Showing Status of XSD Validation | inside console/ log |
                  | CALCULATION VALIDATION | Showing Status of Calculation Validation | inside console/ log |
                  | REGULATION VALIDATION | Showing Status of Regulation Validation | inside console/ log |

    - ### sign:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | xml-path | E-Invoice XML file Path Compliance with UBL 2.1 | /home/orgs/sdk/invoice.xml |
              | private-key-path | Private Key Path Encrypted Generated from previous steps | /home/orgs/sdk/output/private.pem |
              | certificate-path | Certificate Path Encrypted Generated from previous Steps | /home/orgs/sdk/output/production_csid.cert |
              | output-path | Signed XML output path | /home/orgs/sdk/output/signed_invoice.xml |

            - output:

                - | output Name | Value | location |
                                      | --- | --- | --- |
                  | Invoice Hash | Hash Of Invoice | inside console/ log |
                  | QR Code | QR code Generated from Invoice Information | inside console/ log |
                  | Signed XML | final Signed XML Generated and written on output path | inside output path |

    - ### generate-qr:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | xml-path | E-Invoice XML file Path Compliance with UBL 2.1 | /home/orgs/sdk/invoice.xml |
              | private-key-path | Private Key Path Encrypted Generated from previous steps | /home/orgs/sdk/output/private.pem |
              | certificate-path | Certificate Path Encrypted Generated from previous Steps | /home/orgs/sdk/output/production_csid.cert |

            - output:

                - | output Name | Value | location |
                                      | --- | --- | --- |
                  | Invoice Hash | Hash Of Invoice | inside console/ log |
                  | QR Code | QR code Generated from Invoice Information | inside console/ log |

    - ### submit:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | signed-xml-path | E-Invoice Signed XML file Path Compliance with UBL 2.1 | /home/orgs/sdk/signed_invoice.xml |
              | production-certificate-response-path | json file response from previous steps Encrypted | /home/orgs/sdk/output/production_response.json |
              | output-path | output path (Where the response should be written) | /home/orgs/sdk/output/submit_response.json |

            - output:

                - | output Name | Value | location |
                                      | --- | --- | --- |
                  | Response | Response from fotara | inside console/ log |
                  | file response | json file written contain the response from fotara | inside output path |

    - ### decrypt:

        - args:

            - | Arg Name | Value | Example |
                              | --- | --- | --- |
              | encrypted-file-path | Encrypted filed generated by the sdk | /home/orgs/sdk/private.pem |

            - output:

                - | output Name | Value | location |
                                      | --- | --- | --- |
                  | decrypted file | Decrypted file plain text will be shown on the logs/ console only | inside console/ log |
