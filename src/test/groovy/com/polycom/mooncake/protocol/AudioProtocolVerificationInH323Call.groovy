package com.polycom.mooncake.protocol

import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.gs.AudioType
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by taochen on 2019-03-28.
 */
class AudioProtocolVerificationInH323Call extends MoonCakeSystemTestSpec {
    @Shared
    GroupSeries groupSeries

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        moonCake.enableH323()
        moonCake.setEncryption("off")
    }

    def cleanupSpec() {
        groupSeries.api().setStereoEnabled(true)
        testContext.releaseSut(groupSeries)
    }

    @Unroll
    def "Verify Audio Protocol In H323 Call With Audio Protocol #expectedProtocol And In Call Rate #callRate Kbps"(AudioType protocol,
                                                                                                                   String expectedProtocol,
                                                                                                                   int callRate,
                                                                                                                   String arxRate,
                                                                                                                   String atxRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        groupSeries.resetAudioProtocol()
        groupSeries.api().setStereoEnabled(true)

        when: "Set the GroupSeries audio protocol"
        if (protocol != null) {
            groupSeries.setAudioProtocol(protocol)
        } else {
            groupSeries.resetAudioProtocol()
            groupSeries.api().setStereoEnabled(false)
        }

        then: "MoonCake place call to the GroupSeries with various call rate and then verify the media statistics"
        logger.info("===============Start H323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            moonCake.placeCall(groupSeries, CallType.H323, callRate)
        }

        then: "Verify the media statistics during the call"
        if (protocol == AudioType.G7221_16 || protocol == AudioType.G7221_24 || AudioType.G7221C_24) {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        } else {
            verifyMediaStatistics(MediaChannelType.ARX, "${expectedProtocol}:--:${arxRate}:--:--:--")
        }
        verifyMediaStatistics(MediaChannelType.ATX, "${expectedProtocol}:--:${atxRate}:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        cleanup:
        moonCake.hangUp()
        pauseTest(2)

        where:
        [protocol, expectedProtocol, callRate, arxRate, atxRate] << getTestData()
    }

    def getTestData() {
        def rtn = []
        callRateList.each {
            rtn << [AudioType.G711A, "G.711A", it, 64, 64]
            rtn << [AudioType.G711U, "G.711U", it, 64, 64]
            rtn << [AudioType.G7221_16, "G.722.1", it, "--", 16]
            rtn << [AudioType.G7221_24, "G.722.1", it, "--", 24]
            rtn << [AudioType.G7221_32, "G.722.1", it, 32, 32]
            rtn << [AudioType.G7221C_24, "G.722.1C", it, "--", 24]
            rtn << [AudioType.G7221C_32, "G.722.1C", it, 32, 32]
            rtn << [AudioType.G7221C_48, "G.722.1C", it, 48, 48]
            rtn << [null, "SirenLPR", it, 64, 64]
        }
        return rtn
    }
}
