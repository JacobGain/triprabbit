# U.S. export compliance review

Review date: September 22, 2026

This is an engineering classification record for release preparation, not legal
advice. The publisher remains responsible for the Google Play U.S. export-laws
declaration and should obtain export counsel if the facts or distribution model
change.

## Current technical facts

- TripRabbit is an Android odometer-history utility distributed as compiled object
  code.
- It declares no Internet permission and provides no network, messaging, cloud,
  VPN, authentication, payment, DRM, password-manager, or remote-access feature.
- It implements no custom or non-standard cryptography and includes no crypto
  library as an application dependency.
- Its release is digitally signed as required by Android/Google Play. Platform
  security, Android's app sandbox, and store signing are not user-facing
  encryption functionality supplied by TripRabbit.
- JSON, CSV, and PDF exports are not encrypted by TripRabbit.

## Preliminary conclusion

On these facts, TripRabbit does not appear to be an encryption item and no
encryption classification request or annual self-classification report appears
to be triggered by this release. A non-encryption consumer app is generally
treated as EAR99 if it is subject to the Export Administration Regulations.
EAR99 does not mean unrestricted: embargoes, sanctioned destinations and
parties, prohibited end uses, and other U.S. controls can still apply.

This conclusion is deliberately narrower than saying the app is "not subject
to the EAR." Country of origin, publisher location, contributors, hosted source
code, and distribution facts affect jurisdiction and are not established by
the Android source tree.

## Publisher actions before distribution

1. Confirm the publisher's country, the app/source origin, and whether the app
   is subject to the EAR.
2. Confirm the developer and payment profiles and selected Play distribution
   countries do not create sanctions or restricted-party issues. Do not rely on
   Google Play alone for this determination.
3. Keep this review with the released version and truthfully acknowledge the
   U.S. export-laws declaration in Play Console only after those checks.
4. Seek qualified export counsel or an official classification if any fact is
   uncertain. Do not use this repository note as a legal opinion.

## Changes that require a new review

Block release and reclassify before adding or materially changing:

- encryption of backups, databases, messages, or network traffic;
- TLS/network libraries beyond ordinary platform use, custom cryptography, or
  cryptographic libraries/APIs;
- login, authentication, password storage, key management, digital signatures,
  VPN, secure messaging, remote access, DRM, payments, or cryptocurrency;
- cloud sync, servers, source-code publication, paid distribution, or a change
  in publisher/exporter, origin, hosting, or destination countries.

If encryption is introduced, determine whether an exclusion applies or whether
the software is classified under Category 5 Part 2 (commonly 5D002 or mass-
market 5D992.c) and whether a BIS classification, notification, or annual self-
classification report is required. Public availability alone does not remove
every pre-publication classification/reporting step for mass-market encryption.

## Official references checked September 22, 2026

- BIS encryption guidance: https://www.bis.gov/learn-support/encryption-controls
- Encryption items not subject to the EAR: https://www.bis.gov/learn-support/encryption-controls/encryption-items-not-subject-to-ear
- BIS Encryption FAQs: https://www.bis.gov/media/documents/encryption-faqs
- EAR Commerce Control List, Category 5 Part 2: https://www.ecfr.gov/current/title-15/subtitle-B/chapter-VII/subchapter-C/part-774/appendix-Supplement%20No.%201%20to%20Part%20774
- OFAC sanctions programs and country information: https://ofac.treasury.gov/sanctions-programs-and-country-information
