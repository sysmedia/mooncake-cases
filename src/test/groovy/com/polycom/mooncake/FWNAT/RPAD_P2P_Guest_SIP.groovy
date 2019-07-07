package com.polycom.mooncake.FWNAT

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import polycom.serviceapi.camera.ContentState
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 6/17/2019
 */
class RPAD_P2P_Guest_SIP extends MoonCakeSystemTestSpec {
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
        groupSeries.enableSIP()
        moonCake.enableSIP()
        dma = testContext.bookSut(Dma.class, "FWNAT")
        mc_sip = generateDialString(moonCake).sipUri
        gs_sip = generateDialString(groupSeries).sipUri
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify external mooncake sip guest call internal gs"() {
        setup:
        moonCake.setEncryption(aes)
        moonCake.registerSip(protocol, false, "", "", "", "", "")
        groupSeries.setEncryption(gsaes)
        groupSeries.registerSip(gs_sip, "polycom.com", "", dma.ip, gsProtocol)
        pauseTest(5)


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        logger.info("===============Start SIP Call with call rate " + 2048 + "===============")
        moonCake.placeCall(gs_sip + "@" + rpad_ip, CallType.SIP, 2048)
        pauseTest(15)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:$isEncrypt")

        and: "Verify the group series media statistics during the call"
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVTX.channelType
        }.rateUsed > 0
        groupSeries.getMediaStatistics().channels.find {
            it.channelType == MediaChannelType.PVRX.channelType
        }.rateUsed > 0
        logger.info("===============Successfully start SIP Call with call rate " + 2048 + "===============")

        when: "Push content on the mooncake"
        moonCake.sendFECC("LEFT")
        pauseTest(3)
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:$isEncrypt")

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
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:$isEncrypt")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:$isEncrypt")

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
        pauseTest(10)

        where:
        aes    | protocol | gsaes | isEncrypt | gsProtocol
        "off"  | "TCP"    | "no"  | "false"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP
        "off"  | "UDP"    | "no"  | "false"   | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_UDP
        "auto" | "TLS"    | "yes" | "true"    | SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS
    }
}
