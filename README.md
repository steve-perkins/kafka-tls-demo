kafka-tls-demo
==============



Setup
=====

Run Vault
---------
* Download [Vault](https://www.vaultproject.io/downloads.html), and startup an instance in dev mode:

```
vault server -dev
```

* In another shell, run these Vault commands:

```
set VAULT_ADDR=http://127.0.0.1:8200
```

... and then:

```
vault auth-enable userpass
vault policy-write writers writers.hcl
vault write auth/userpass/users/vault_user password=vault_pass policies=writers
vault mount pki
vault mount-tune -max-lease-ttl=87600h pki
vault write pki/root/generate/internal common_name=myvault.com ttl=87600h
vault write pki/roles/kafka-broker allowed_domains="example.com" allow_subdomains="true" max_ttl="72h"
vault write pki/roles/kafka-consumer allowed_domains="example.com" allow_subdomains="true" max_ttl="72h"
vault write pki/roles/kafka-producer allowed_domains="example.com" allow_subdomains="true" max_ttl="72h"
vault write pki/issue/kafka-broker common_name=kafka-broker.example.com
```

The `writers.hcl` file is located in the root of this repo.

Create Root CA
--------------

* That last Vault command will write to the console three blocks of text in PEM format.  Copy those three blocks into 
  text files named `certificate.pem`, `issuing_ca.pem`, and `private_key.pem`.

* Convert the PEM-formatted private key and certificate chain issued by Vault into a PCKS#12 keystore:

```
openssl pkcs12 -export -out keystore.pkcs12 -in certificate.pem -inkey private_key.pem
```

* Convert the PCKS#12 keystore into a Java keystore:

```
keytool -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststorepass <password from previous step>
```

* Convert the PEM-formatted CA certificate into binary DER format:

```
openssl x509 -in issuing_ca.pem -out ca.der -outform der
```

* Convert the DER file into a Java truststore:

```
keytool.exe -importcert -alias CARoot -file ca.der -keystore truststore.jks
```

Setup Kafka broker
------------------

* Download [Apache Kafka](https://kafka.apache.org/downloads), and unzip it somewhere on your filesystem.

* Copy the `keystore.jks` and `truststore.jks` files generated above to the root directory of your Kafka installation.

* Open up `config/server.properties` in a text editor, and add the following lines:

```
listeners=SSL://:9093
security.inter.broker.protocol=SSL

ssl.keystore.location=keystore.jks
ssl.keystore.password=password
ssl.truststore.location=truststore.jks
ssl.truststore.password=password
ssl.client.auth=required
```

(This assumes you used "password" as the password for your `keystore.jks` and `truststore.jks` files above.)

* In a command-line shell, from the Kafka root directory, start a ZooKeeper instance:

```
bin\zookeeper-server-start.sh config\zookeeper.properties
```
or
```
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

* In another shell, startup the Kafka broker:

```
bin\kafka-server-start.sh config\server.properties
```
or
```
bin\windows\kafka-server-start.bat config\server.properties
```

* In yet another shell, create a "test" Kafka topic:

```
bin\kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```
or
```
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```

Run Kafka producer and consumer
-------------------------------

* Run the `com.steveperkins.tls.ProducerApp` class in this project.  Verify that it connects to Kafka, and that 
  every couple of seconds it is writing to the console that it just produced a new message.
  
* Run the `com.steveperkins.tls.ConsumerApp` class in this project.  Verify that it connects to Kafka, and that 
  every couple of seconds it is writing to the console that it just received a new message produced by `ProducerApp`.
  
* Terminate both processes once you are done.


