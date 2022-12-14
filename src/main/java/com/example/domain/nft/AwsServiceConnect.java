package com.example.domain.nft;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AwsServiceConnect {
    private static String Seoul_Region = "ap-northeast-2";
    private static String KEYARN = "arn:aws:kms:ap-northeast-2:905126260776:key/2af69298-b2ea-476a-8636-a9c039f16159";
    private static AWSKMS kmsClient = AWSKMSClientBuilder.standard().build();
    private static AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                    "https://s3.ap-northeast-2.amazonaws.com",
                    Seoul_Region))
            .withCredentials(new ProfileCredentialsProvider())
            .build();
    private static final byte[] EXAMPLE_DATA = "Ji Seung Gu - AWS Encryption SDK".getBytes(StandardCharsets.UTF_8);
    private static final String KEY_ID = "alias/privateproject";
    private static final String KEY_SPEC = "AES_256";
    public static final AwsCrypto crypto = AwsCrypto.standard();

    public static void main(String[] args) throws IOException, ParseException {
//        KmsTest_MakeName();
//        Aws_UsingS3("0x0C94A0E9E4a5A3e2829389b1572b8176209670c1");
//        encryptAndDecrypt_EncryptionSDK("6fb4c3dd41ff6dcdd6bb372f1b2ba252c2da5f51f42212b848f858db7e35de01".getBytes(StandardCharsets.UTF_8),"ETH","wallet");


        // * S3??? ????????? ???????????? private ???????????? (byte????????? ???????????? ????????? new String(...) ??????
        byte[] jsonStringData = GetAwsS3Data("0x0C94A0E9E4a5A3e2829389b1572b8176209670c1");
        System.out.println("jsonStringData :" + jsonStringData);
        System.out.println("jsonStringData :" + new String(jsonStringData, StandardCharsets.UTF_8));
        System.out.println("jsonStringData :" + Base64.getEncoder().encodeToString(jsonStringData));
        String temp = new String(jsonStringData, StandardCharsets.UTF_8);
        JSONObject jsonObject = StringToJson(temp);
        System.out.println("jsObject1 :" + jsonObject.get("ciphertext"));

        // * String????????? byte????????? ???????????? ?????????
        byte[] bytes = StringToByte(jsonObject.get("ciphertext"));

//        String temp2 = jsonObject.get("ETH").toString();
//        encryptAndDecrypt_DecryptionSDK(temp.getBytes(StandardCharsets.UTF_8),"ETH",temp2);
        encryptAndDecrypt_DecryptionSDK(bytes, "ETH", "wallet");
    }
    public static byte[] StringToByte(Object before) {
        String response = before.toString();
        String[] byteValues = response.substring(1, response.length()-1).split(",");
        byte[] bytes = new byte[byteValues.length];

        for (int i=0, len = bytes.length; i < len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }

        return bytes;
    }
    public static void test() {
//         Instantiate the SDK
//        final AwsCrypto crypto = AwsCrypto.standard();

        // Set up the master key provider
        final KmsMasterKeyProvider prov = KmsMasterKeyProvider.builder().buildStrict(KEYARN);

        // Set up the encryption context
        // NOTE: Encrypted data should have associated encryption context
        // to protect its integrity. This example uses placeholder values.
        // For more information about the encryption context, see
        // https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/concepts.html#encryption-context
        final Map<String, String> context = Collections.singletonMap("ExampleContextKey", "ExampleContextValue");

        // Encrypt the data
        //
        final CryptoResult<byte[], KmsMasterKey> encryptResult = crypto.encryptData(prov, EXAMPLE_DATA, context);
        final byte[] ciphertext = encryptResult.getResult();
        System.out.println("Ciphertext: " + Arrays.toString(ciphertext));
        System.out.println("ciphertext: " + ciphertext);

//        String response = "[-47, 1, 16, 84, 2, 101, 110, 83, 111, 109, 101, 32, 78, 70, 67, 32, 68, 97, 116, 97]";      // response from the Python script
        String response2 = Arrays.toString(ciphertext);      // response from the Python script
        String[] byteValues = response2.substring(1, response2.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];

        for (int i = 0, len = bytes.length; i < len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }
        System.out.println("??????:" + Arrays.toString(bytes));
        System.out.println(bytes);

        // Decrypt the data
        final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(prov, bytes);
        // Your application should verify the encryption context and the KMS key to
        // ensure this is the expected ciphertext before returning the plaintext
        if (!decryptResult.getMasterKeyIds().get(0).equals(KEYARN)) {
            throw new IllegalStateException("Wrong key id!");
        }

        // The AWS Encryption SDK may add information to the encryption context, so check to
        // ensure all of the values that you specified when encrypting are *included* in the returned encryption context.
        if (!context.entrySet().stream()
                .allMatch(e -> e.getValue().equals(decryptResult.getEncryptionContext().get(e.getKey())))) {
            throw new IllegalStateException("Wrong Encryption Context!");
        }

        // The data is correct, so return it.
        System.out.println("Decrypted: " + new String(decryptResult.getResult(), StandardCharsets.UTF_8));

    }

    public static JSONObject StringToJson(String data) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(data);
        JSONArray jsonArray = (JSONArray) jsonObject.get("data");
        JSONObject temp = (JSONObject) jsonArray.get(0);

        System.out.println("jsonData  " + jsonObject.get("data"));

        return temp;
    }

    public static String encryptAndDecrypt_EncryptionSDK(byte[] PLAINTEXT, String Key, String Value) {

//        final AwsCrypto crypto = AwsCrypto.builder()
//                .withCommitmentPolicy(CommitmentPolicy.RequireEncryptAllowDecrypt)
//                .build();
        // ????????? ??????
        final KmsMasterKeyProvider keyProvider = KmsMasterKeyProvider.builder().buildStrict(KEYARN);

        // ????????? ???????????? = ?????? ?????????, ????????? ???????????? ???????????? ????????? ???????????? ????????? ??????
        Map<String, String> encryptionContext = Collections.singletonMap(Key, Value);


        final CryptoResult<byte[], KmsMasterKey> encryptResult = crypto.encryptData(keyProvider,
                PLAINTEXT, encryptionContext);
        final byte[] ciphertext_byte = encryptResult.getResult();
        System.out.println("encryptResult.getMasterKeyIds() :" + encryptResult.getMasterKeyIds());
        System.out.println("encryptResult.getMasterKeys() :" + encryptResult.getMasterKeys());
        System.out.println("encryptResult.getEncryptionContext() :" + encryptResult.getEncryptionContext());
        System.out.println("encryptResult.getHeaders().getEncryptionContext() :" + encryptResult.getHeaders().getEncryptionContext());
        System.out.println("encryptResult.getHeaders().getEncryptionContextMap() :" + encryptResult.getHeaders().getEncryptionContextMap());
        //???????????? ????????? ????????? ???
        System.out.println("2. CIPHERTEXT :" + ciphertext_byte);
        //????????? ????????????
        System.out.println("3. ENCRYPT_CONTEXT :" + encryptResult.getEncryptionContext());

        String test = Base64.getEncoder().encodeToString(ciphertext_byte);
        System.out.println("Base64 : " + test);
        String test2 = new String(test.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        System.out.println("Base64 -> new String :" + test2);

        String ciphertext = Arrays.toString(ciphertext_byte);
        System.out.println(ciphertext);
        return ciphertext;
    }

    public static String encryptAndDecrypt_DecryptionSDK(byte[] CIPHERTEXT, String Key, String Value) {
        System.out.println("CIPHERTEXT :" + CIPHERTEXT);
        // ????????? ??????
        final KmsMasterKeyProvider keyProvider = KmsMasterKeyProvider.builder().buildStrict(KEYARN);
        System.out.println("1");
        //????????? ????????????
        Map<String, String> encryptionContext = Collections.singletonMap(Key, Value);
        System.out.println("2");
        //?????????
        final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(keyProvider, CIPHERTEXT);
        System.out.println("3");
        //????????? ????????? ???
        System.out.println("4. DECRYPTRESULT :" + decryptResult.getResult());

        if (!encryptionContext.entrySet().stream()
                .allMatch(
                        e -> e.getValue().equals(decryptResult.getEncryptionContext().get(e.getKey())))) {
            throw new IllegalStateException("Wrong Encryption Context!");
        }

        // 7. Verify that the decrypted plaintext matches the original plaintext
        assert Arrays.equals(decryptResult.getResult(), EXAMPLE_DATA);

        System.out.println("5. PLAIN_TEXT: " + new String(decryptResult.getResult(), StandardCharsets.UTF_8));

        return new String(decryptResult.getResult(), StandardCharsets.UTF_8);
    }

    public static byte[] GetAwsS3Data(String waddress) throws IOException {
        S3Object o = s3.getObject("privateproject-s3", waddress + ".json");
        S3ObjectInputStream s3io = o.getObjectContent();

        return s3io.readAllBytes();
    }

    public static void Aws_UsingS3(String waddress, String CIPHERTEXT, String Key, String Value) {
        List<Bucket> buckets = s3.listBuckets();
        System.out.println("Your {S3} buckets are:");
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject ResultJson = new JSONObject();
        jsonObject.put(Key, Value);
        jsonObject.put("ciphertext", CIPHERTEXT);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        ResultJson.put("data", jsonArray);

        s3.putObject("privateproject-s3", waddress + ".json", ResultJson.toString());
    }

    public void KMS_create_DataKEY() {
        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
        dataKeyRequest.setKeyId(KEY_ID);
        dataKeyRequest.setKeySpec(KEY_SPEC);

        GenerateDataKeyResult dataKeyResult = kmsClient.generateDataKey(dataKeyRequest);

        ByteBuffer plaintextKey = dataKeyResult.getPlaintext();
        //KMS CMK??? ????????? ????????? ???
        System.out.printf(
                "Successfully generated an plaintextKey: %s%n",
                Base64.getEncoder().encodeToString(plaintextKey.array())
        );
        ByteBuffer encryptedKey = dataKeyResult.getCiphertextBlob();

        //KMS CMK??? ????????? ???????????? ????????? ???
        System.out.printf(
                "Successfully generated an encrypted data key: %s%n",
                Base64.getEncoder().encodeToString(encryptedKey.array())
        );
    }

    public void KMS_Data_Key_Dec(ByteBuffer encriptedText) {
        System.out.println("KMS_Data_Key_Dec Start");
        System.out.println();

        DecryptRequest request = new DecryptRequest()
                .withKeyId(KEY_ID)
                .withCiphertextBlob(encriptedText);

        ByteBuffer plainText = kmsClient
                .decrypt(request)
                .getPlaintext();

        System.out.println(new String(plainText.array()));
    }

    public void KMS_Data_Key_enc() {
        System.out.println("KMS_Data_Key_enc Start");
        System.out.println();
        ByteBuffer plaintext = ByteBuffer.wrap(EXAMPLE_DATA);

        EncryptRequest req = new EncryptRequest()
                .withKeyId(KEY_ID)
                .withPlaintext(plaintext);

        ByteBuffer ciphertext = kmsClient
                .encrypt(req)
                .getCiphertextBlob();

        ByteBuffer ciphertextBlob = ciphertext;

        System.out.println("========================");
        System.out.println("encode Base 64 CipherTextBlob :" + new String(Base64.getEncoder().encodeToString(ciphertext.array())));
        System.out.println("========================");

        KMS_Data_Key_Dec(ciphertext);

    }

    // KMS ?????? ?????????
    public void KmsTest_MakeName() {

        // Create an alias for a KMS key
        //arn:aws:kms:ap-northeast-2:905126260776:key/2af69298-b2ea-476a-8636-a9c039f16159
        String aliasName = "alias/privateproject2";
        // Replace the following example key ARN with a valid key ID or key ARN
        String targetKeyId = "2af69298-b2ea-476a-8636-a9c039f16159";

        CreateAliasRequest req = new CreateAliasRequest().withAliasName(aliasName).withTargetKeyId(targetKeyId);
        kmsClient.createAlias(req);
    }
}
