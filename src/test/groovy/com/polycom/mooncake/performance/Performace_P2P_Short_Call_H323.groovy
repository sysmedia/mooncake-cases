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
 * Created by nshi on 5/21/2019.
 *  *
 * SUT call with highest Tx and Rx video/content resolution, and with encryption on
 * Monitor CPU, Memory and temperature during the test
 * Repeat short call for many times
 * Repeat send and receive content for many times
 * Repeat content war for many times
 * Repeat send and receive FECC for many times
 */

@Performance(runTimes = 10)
class Performace_P2P_Short_Call_H323 extends MoonCakeSystemTestSpec {
    @Shared
    PerfDataProbe probe

    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String h323name

    @Shared
    String h323e164

    @Shared
    String gs_h323name

    @Shared
    String gs_e164

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("yes")
        groupSeries.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("yes")
        dma = testContext.bookSut(Dma.class, "SAT")
        probe = testContext.addPerfDataProbe(moonCake, 10000)
        def mcdialString = generateDialString(moonCake)
        def gsdialString = generateDialString(groupSeries)
        h323name = mcdialString.h323Name
        h323e164 = mcdialString.e164Number
        gs_h323name = gsdialString.h323Name
        gs_e164 = gsdialString.e164Number
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify H323 P2P call with registration with dial string #dialString and call rate #callRate Kbps"(String dialString, int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake H323 registered and Group Series H323 registered"
        moonCake.registerGk(true, false, dma.ip, h323name, h323e164, "", "")
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)

        then: "MoonCake place call to the GroupSeries with various dial string and call rate"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.H323, callRate)
        }

        then: "Push content on the mooncake and verify the media statistics during the call"
        1.upto(5) {
            moonCake.pushContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
            moonCake.stopContent()
        }


        then: "Push content on the GS and verify the media statistics during the call"
        1.upto(5) {
            groupSeries.playHdmiContent()
            pauseTest(10)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
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
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

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
        [dialString, callRate] << getTestData()
    }

    /**
     * Create a list of different dial string:
     * "h323name or h323extension"@DMA_IP
     * "h323name or h323extension"@FE_Host
     * "h323name or h323extension" @FE_FQDN
     * @return
     */

    def callList = [2048, 4096] as int[]

    def getTestData() {
        def rtn = []
        callList.each {
            rtn << [gs_h323name, it]
            rtn << [gs_e164 + "@" + groupSeries.hostName, it]
        }
        return rtn
    }


}
