package com.polycom.mooncake.performance

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.performance.PerfDataProbe
import com.polycom.honeycomb.test.performance.Performance
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-05-14.
 *
 * SUT call with highest Tx and Rx video/content resolution, and with encryption on
 * Monitor CPU, Memory and temperature during the test
 * Repeat short call for many times
 * Repeat send and receive content for many times
 * Repeat content war for many times
 * Repeat send and receive FECC for many times
 */
@Performance(runTimes = 10)
class Performance_P2P_Short_Call_SIP extends MoonCakeSystemTestSpec {
    @Shared
    PerfDataProbe probe

    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String sipUri

    @Shared
    String gs_sip_username

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("yes")
        groupSeries.enableSIP()
        moonCake.enableSIP()
        moonCake.setEncryption("yes")
        dma = testContext.bookSut(Dma.class, keyword)
        probe = testContext.addPerfDataProbe(moonCake, 10000)
        sipUri = generateDialString(moonCake).sipUri
        gs_sip_username = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Short SIP P2P call with registered endpoints with dial string #dialString and call rate #callRate Kbps and transport #sipTransProtocol "(String dialString,
                                                                                                                                                  String sipTransProtocol,
                                                                                                                                                  int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        moonCake.registerSip(sipTransProtocol, true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start SIP Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
        }

        then: "Push content on the mooncake for 5 times"
        1.upto(5) {
            moonCake.pushContent()
            pauseTest(10)

            then: "Verify the media statistics during the call"
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            then: "Stop the content"
            moonCake.stopContent()
        }

        then: "GroupSeries push content for 5 times"
        1.upto(5) {
            groupSeries.playHdmiContent()
            pauseTest(10)

            then: "Verify the media statistics during the call"
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            then: "Stop the content"
            groupSeries.stopContent()
        }

        then: "Repeat content war for several times"
        1.upto(5) {
            moonCake.pushContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

            groupSeries.playHdmiContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        }
        logger.info("===============Successfully start SIP Call with call rate " + callRate + "===============")

        then: "Sending the FECC on the MoonCake"
        1.upto(2) {
            String option
            int opt = getRandomIntegerNumberInRange(1, 8)
            if (opt == 1) {
                option = "up"
            } else if (opt == 2) {
                option = "down"
            } else if (opt == 3) {
                option = "left"
            } else if (opt == 4) {
                option = "right"
            } else if (opt == 5) {
                option = "zoomin"
            } else if (opt == 6) {
                option = "zoomout"
            } else if (opt == 7) {
                option = "zoom+"
            } else if (opt == 8) {
                option = "zoom-"
            }
            moonCake.sendFECC(option)
        }

        then: "Receiving the FECC on the MoonCake"
        1.upto(2) {
            String option
            int opt = getRandomIntegerNumberInRange(1, 6)
            if (opt == 1) {
                option = "zoomout"
            } else if (opt == 2) {
                option = "zoomin"
            } else if (opt == 3) {
                option = "left"
            } else if (opt == 4) {
                option = "right"
            } else if (opt == 5) {
                option = "up"
            } else if (opt == 6) {
                option = "down"
            }
            groupSeries.sendFECC(option)
        }

        cleanup:
        moonCake.hangUp()
        pauseTest(5)

        where:
        [dialString, sipTransProtocol, callRate] << getTestData()
    }

    /**
     * Create a list of different dial string:
     * SIPURI@FE_IP, SIPURI@DMA_IP, SIPURI@FE_Host
     * SIPURI@FE_FQDN
     * @return
     */
    def getTestData() {
        def rtn = []
        rtn << [gs_sip_username + "@" + groupSeries.ip, "TCP", 4096]
        return rtn
    }

}
