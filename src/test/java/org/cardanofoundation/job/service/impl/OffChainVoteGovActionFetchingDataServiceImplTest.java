package org.cardanofoundation.job.service.impl;

import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainFetchResult;

@SpringBootTest(classes = {OffChainVoteGovActionFetchingDataServiceImpl.class})
@FieldDefaults(level = AccessLevel.PRIVATE)
class OffChainVoteGovActionFetchingDataServiceImplTest {

  OffChainVoteGovActionFetchingDataServiceImpl offChainVoteGovActionFetchingDataServiceImpl;

  @BeforeEach
  void init() {
    offChainVoteGovActionFetchingDataServiceImpl =
        new OffChainVoteGovActionFetchingDataServiceImpl();
  }

  @Test
  @DisplayName(
      "Should extract raw off-chain data from OffChainFetchResult object when valid raw json data)")
  void test_extractRawOffChainData_whenJsonRawDataIsValid() {
    // preparing
    String jsonRawData =
        "{\n"
            + "  \"@context\": {\n"
            + "    \"@language\": \"en-us\",\n"
            + "    \"CIP100\": \"https://github.com/cardano-foundation/CIPs/blob/master/CIP-0100/README.md#\",\n"
            + "    \"CIP108\": \"https://github.com/cardano-foundation/CIPs/blob/master/CIP-0108/README.md#\",\n"
            + "    \"hashAlgorithm\": \"CIP100:hashAlgorithm\",\n"
            + "    \"body\": {\n"
            + "      \"@id\": \"CIP108:body\",\n"
            + "      \"@context\": {\n"
            + "        \"references\": {\n"
            + "          \"@id\": \"CIP108:references\",\n"
            + "          \"@container\": \"@set\",\n"
            + "          \"@context\": {\n"
            + "            \"GovernanceMetadata\": \"CIP100:GovernanceMetadataReference\",\n"
            + "            \"Other\": \"CIP100:OtherReference\",\n"
            + "            \"label\": \"CIP100:reference-label\",\n"
            + "            \"uri\": \"CIP100:reference-uri\",\n"
            + "            \"referenceHash\": {\n"
            + "              \"@id\": \"CIP108:referenceHash\",\n"
            + "              \"@context\": {\n"
            + "                \"hashDigest\": \"CIP108:hashDigest\",\n"
            + "                \"hashAlgorithm\": \"CIP100:hashAlgorithm\"\n"
            + "              }\n"
            + "            }\n"
            + "          }\n"
            + "        },\n"
            + "        \"title\": \"CIP108:title\",\n"
            + "        \"abstract\": \"CIP108:abstract\",\n"
            + "        \"motivation\": \"CIP108:motivation\",\n"
            + "        \"rationale\": \"CIP108:rationale\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"authors\": {\n"
            + "      \"@id\": \"CIP100:authors\",\n"
            + "      \"@container\": \"@set\",\n"
            + "      \"@context\": {\n"
            + "        \"name\": \"http://xmlns.com/foaf/0.1/name\",\n"
            + "        \"witness\": {\n"
            + "          \"@id\": \"CIP100:witness\",\n"
            + "          \"@context\": {\n"
            + "            \"witnessAlgorithm\": \"CIP100:witnessAlgorithm\",\n"
            + "            \"publicKey\": \"CIP100:publicKey\",\n"
            + "            \"signature\": \"CIP100:signature\"\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  },\n"
            + "  \"hashAlgorithm\": \"blake2b-256\",\n"
            + "  \"body\": {\n"
            + "    \"title\": \"Buy Ryan a island\",\n"
            + "    \"abstract\": \"Withdraw 200000000000 ADA from the treasury so Ryan can buy an island.\",\n"
            + "    \"motivation\": \"The current problem is that Ryan does not have an island, but he would really like an island.\",\n"
            + "    \"rationale\": \"With these funds from the treasury will be sold for **cold hard cash**, this cash can then be used to purchase an island for Ryan. An example of this island is provided in the references.\",\n"
            + "    \"references\": [\n"
            + "      {\n"
            + "        \"@type\": \"Other\",\n"
            + "        \"label\": \"A cool island for Ryan\",\n"
            + "        \"uri\": \"https://www.google.com/maps/place/World's+only+5th+order+recursive+island/@62.6511465,-97.7946829,15.75z/data=!4m14!1m7!3m6!1s0x5216a167810cee39:0x11431abdfe4c7421!2sWorld's+only+5th+order+recursive+island!8m2!3d62.651114!4d-97.7872244!16s%2Fg%2F11spwk2b6n!3m5!1s0x5216a167810cee39:0x11431abdfe4c7421!8m2!3d62.651114!4d-97.7872244!16s%2Fg%2F11spwk2b6n?authuser=0&entry=ttu\"\n"
            + "      }\n"
            + "    ]\n"
            + "  },\n"
            + "  \"authors\": [\n"
            + "    {\n"
            + "      \"name\": \"Ryan Williams\",\n"
            + "      \"witness\": {\n"
            + "        \"witnessAlgorithm\": \"ed25519\",\n"
            + "        \"publicKey\": \"7ea09a34aebb13c9841c71397b1cabfec5ddf950405293dee496cac2f437480a\",\n"
            + "        \"signature\": \"a476985b4cc0d457f247797611799a6f6a80fc8cb7ec9dcb5a8223888d0618e30de165f3d869c4a0d9107d8a5b612ad7c5e42441907f5b91796f0d7187d64a01\"\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";

    OffChainFetchResult offChainFetchResult =
        OffChainFetchResult.builder()
            .anchorHash("anchorHash")
            .anchorUrl("anchorUrl")
            .rawData(jsonRawData)
            .build();

    OffChainVoteGovActionData offChainVoteGovActionData =
        offChainVoteGovActionFetchingDataServiceImpl.extractRawOffChainData(offChainFetchResult);
    Assertions.assertEquals("Buy Ryan a island", offChainVoteGovActionData.getTitle());
    Assertions.assertEquals(
        "Withdraw 200000000000 ADA from the treasury so Ryan can buy an island.",
        offChainVoteGovActionData.getAbstractData());
    Assertions.assertEquals(
        "The current problem is that Ryan does not have an island, but he would really like an island.",
        offChainVoteGovActionData.getMotivation());
    Assertions.assertEquals(
        "With these funds from the treasury will be sold for **cold hard cash**, this cash can then be used to purchase an island for Ryan. An example of this island is provided in the references.",
        offChainVoteGovActionData.getRationale());
  }

  @Test
  @DisplayName(
      "Should extract raw off-chain data from OffChainFetchResult object when invalid raw json data)")
  void test_extractRawOffChainData_whenJsonRawDataIsInvalid() {
    OffChainFetchResult offChainFetchResult =
        OffChainFetchResult.builder()
            .anchorHash("anchorHash")
            .anchorUrl("anchorUrl")
            .rawData("invalid json data")
            .build();

    OffChainVoteGovActionData offChainVoteGovActionData =
        offChainVoteGovActionFetchingDataServiceImpl.extractRawOffChainData(offChainFetchResult);
    Assertions.assertNull(offChainVoteGovActionData.getTitle());
    Assertions.assertNull(offChainVoteGovActionData.getAbstractData());
    Assertions.assertNull(offChainVoteGovActionData.getMotivation());
    Assertions.assertNull(offChainVoteGovActionData.getRationale());
  }

  @Test
  void test_extractFetchError() {
    OffChainFetchResult offChainFetchResult =
        OffChainFetchResult.builder()
            .anchorHash("anchorHash")
            .anchorUrl("anchorUrl")
            .isFetchSuccess(false)
            .fetchFailError("fetchFailError message")
            .build();

    OffChainVoteFetchError offChainVoteFetchError =
        offChainVoteGovActionFetchingDataServiceImpl.extractFetchError(offChainFetchResult);
    Assertions.assertEquals("anchorHash", offChainVoteFetchError.getId().getAnchorHash());
    Assertions.assertEquals("anchorUrl", offChainVoteFetchError.getId().getAnchorUrl());
    Assertions.assertEquals("fetchFailError message", offChainVoteFetchError.getFetchError());
  }

  @Test
  void test_crawlOffChainAnchors() {
    // preparing
    Anchor anchor1 = new Anchor();
    anchor1.setAnchorUrl("http://bit.ly/3QFMhii?index=6"); // fetchFailError
    anchor1.setAnchorHash("1111111111111111111111111111111111111111111111111111111111111112");

    Anchor anchor2 = new Anchor();
    anchor2.setAnchorUrl("https://hornan7.github.io/proposal.jsonld"); // fetchSuccess
    anchor2.setAnchorHash("c64b5f660510891a198de8aa15e4b4c1138948cec1e2b5d99164ef3ac33a52e1");
    List<Anchor> anchors = List.of(anchor1, anchor2);

    offChainVoteGovActionFetchingDataServiceImpl.crawlOffChainAnchors(anchors);

    var fetchSuccessLst =
        offChainVoteGovActionFetchingDataServiceImpl.getOffChainAnchorsFetchSuccess();
    var fetchErrorLst = offChainVoteGovActionFetchingDataServiceImpl.getOffChainAnchorsFetchError();

    Assertions.assertEquals(1, fetchSuccessLst.size());
    Assertions.assertEquals(1, fetchErrorLst.size());
    Assertions.assertEquals(
        "https://hornan7.github.io/proposal.jsonld", fetchSuccessLst.get(0).getAnchorUrl());
    Assertions.assertEquals("http://bit.ly/3QFMhii?index=6", fetchErrorLst.get(0).getAnchorUrl());
  }
}
