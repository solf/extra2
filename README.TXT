This is a WORK-IN-PROGRESS to make a proper open-source project out of the
collection of the various support code that I've written over the years.




ATTENTION: this project uses Lombok: https://projectlombok.org/
To use it in IDE you need to install Lombok for your IDE (developed using
version 1.18.10).

Contains various useful utilities that are intended to be used by other projects.

Look into specific package-info.java files for more information.


Fixing TestNG DTD errors in Eclipse
'Referenced file contains errors (http://testng.org/testng-1.0.dtd)...'

a) Change path to https
b) If after change there's a problem with PKIX certification path -- need to
update JRE to latest (which may include required certs) or install certs manually.

- Open https://testng.org/
- Check certification info, e.g. for 'Sectigo RSA Domain Validation Secure Server CA'
- Check serial number, e.g.: 7D:5B:51:26:B4:76:BA:11:DB:74:16:0B:BC:53:0D:A7
- Go to Sectigo certs page: https://secure.comodo.com/products/publiclyDisclosedSubCACerts
- Download required cert e.g. 7D5B5126B476BA11DB74160BBC530DA7
- keytool -import -trustcacerts -alias sectigo-domain-validation-cert -keystore cacerts -file SectigoRSADomainValidationSecureServerCA.crt
    default password is: changeit
      
