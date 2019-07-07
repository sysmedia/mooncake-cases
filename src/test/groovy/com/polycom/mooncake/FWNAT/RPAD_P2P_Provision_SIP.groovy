package com.polycom.mooncake.FWNAT

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import polycom.serviceapi.camera.ContentState
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 5/27/2019
 */
class RPAD_P2P_Provision_SIP extends MoonCakeSystemTestSpec {
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String mc_sip

    @Shared
    String gs_sip

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")
        groupSeries = testContext.bookSut(GroupSeries.class, "FWNAT")
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, "FWNAT")
        mc_sip = generateDialString(moonCake).sipUri
        gs_sip = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "Verify external mooncake p2p call gs via sip"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        moonCake.registerSip("TCP", true, "polycom.com", rpad_ip, "", mc_sip, "")
        groupSeries.registerSip(gs_sip, "polycom.com", "", dma.ip)
        logger.info("===============Start SIP Call with call rate " + 2048 + "===============")
        moonCake.placeCall(gs_sip, CallType.SIP, 2048)
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
