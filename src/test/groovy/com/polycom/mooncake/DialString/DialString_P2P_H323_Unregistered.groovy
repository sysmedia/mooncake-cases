package com.polycom.mooncake.DialString

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.logCollecting.CollectSutLogIfFailed
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Dancy Li on 2019-04-09.
 * Test different dial string when H323 not registered, standalone mode, SUT will do dial string resolve
 * Check audio, video, content sending and receiving
 * Disable DHCP
 * Test dial out to not registered EP
 * Test dial out to registered EP
 */
@CollectSutLogIfFailed
class DialString_P2P_H323_Unregistered extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        def gsDialString = generateDialString(groupSeries)
        gs_h323name = gsDialString.h323Name
        gs_e164 = gsDialString.e164Number
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify H323 P2P call without registration with dial string #dialString and call rate #callRate Kbps when remote endpoint is H323 not registered"(String dialString,
                                                                                                                                                          int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, callRate] << getTestData_1()
    }

    @Unroll
    def "Verify H323 P2P call without registration with dial string #dialString and call rate #callRate Kbps when remote endpoint is H323 registered"(String dialString,
                                                                                                                                                      int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set Group Series to H323 registered"
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
            pauseTest(10)
        }

        then: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, callRate] << getTestData_2()
    }

    //Create a list of different dial string: FQDN, h323:FQDN

    def getTestData_1() {
        def rtn = []
        def fe_fqdn = groupSeries.getHostName() + "." + groupSeries.getDomainName()
        callRateList.each {
            rtn << [fe_fqdn, it]
            rtn << ["h323:" + fe_fqdn, it]
        }
        return rtn
    }

    /**
     * Create a list of different dial string
     * H323Alias@FE_IP, H323e164@FE_IP, H323Alias@DMA_IP,
     * H323e164@DMA_IP, H323Alias@FE_FQDN, H323e164@DMA_FQDN
     * h323:H323Alias@DMA_IP, h323:H323e164@FE_FQDN
     * @return
     */

    def getTestData_2() {
        def rtn = []
        def fe_fqdn = groupSeries.getHostName() + "." + groupSeries.getDomainName()
        def dma_fqdn = dma.networkSettings.physicalHostName + "." + dma.networkSettings.dnsDomain
        callRateList.each {
            rtn << [gs_h323name + "@" + groupSeries.ip, it]
            rtn << [gs_e164 + "@" + groupSeries.ip, it]
            rtn << [gs_h323name + "@" + dma.ip, it]
            rtn << [gs_e164 + "@" + dma.ip, it]
            rtn << [gs_h323name + "@" + fe_fqdn, it]
            rtn << [gs_e164 + "@" + dma_fqdn, it]
            rtn << ["h323:" + gs_h323name + "@" + dma.ip, it]
            rtn << ["h323:" + gs_e164 + "@" + fe_fqdn, it]
        }
        return rtn
    }
}

