package com.polycom.mooncake.FWNAT

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import polycom.serviceapi.camera.ContentState
import spock.lang.Shared

/**
 * Created by dyuan on 6/8/2019
 */
class RPAD_P2P_Provision_H323 extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    def mc_alias

    @Shared
    def gs_alias

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")
        groupSeries = testContext.bookSut(GroupSeries.class, "FWNAT")
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableH323()
        moonCake.enableH323()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, "FWNAT")
        mc_alias = generateDialString(moonCake)
        gs_alias = generateDialString(groupSeries)
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "Verify external mooncake p2p call gs via h323"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        groupSeries.registerGk(gs_alias.h323Name, gs_alias.e164Number, dma.ip)
        logger.info("===============Start SIP Call with call rate " + 2048 + "===============")

        when: "Set MoonCake place h323 call to gs"
        moonCake.placeCall(gs_alias.h323Name, CallType.H323, 2048)
        pauseTest(15)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        and: "Verify the group series media statistics during the call"
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVTX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVRX.channelType
        }.rateUsed > 0
        logger.info("===============Successfully start SIP Call with call rate " + 2048 + "===============")

        when: "Push content on the mooncake"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:--")

        and: "Verify the group series media statistics MoonCake show content"
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVTX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVRX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.CVRX.channelType
        }.rateUsed > 0

        when: "Push content on the group series"
        groupSeries.playHdmiContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")

        and: "Verify the group series media statistics MoonCake show content"
        groupSeries.contentStatus == ContentState.CONTENT_SENDING
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVTX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVRX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.CVTX.channelType
        }.rateUsed > 0

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }
}
