package com.polycom.mooncake.FWNAT

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
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
 * Created by dyuan on 6/11/2019
 * # 1. h323 registeration
 * # 2. register RPAD with sip in standalone mode
 * # Description: RPAD environment: SIP&H323, mixed VMR, Int&Ext, Privisoned&Registered&Unregistered
 */
class RPAD_MP_MIXED_VMR_RMX extends MoonCakeSystemTestSpec {

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
        groupSeries.setEncryption("yes")
        moonCake.setEncryption("auto")
        dma = testContext.bookSut(Dma.class, "FWNAT")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        mc_alias = generateDialString(moonCake)
        gs_alias = generateDialString(groupSeries)
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Verify mooncake can join #pool_order vmr conference via SIP and GS via H323"() {
        setup:
        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, null, null)
        moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")
        groupSeries.registerGk(gs_alias.h323Name, gs_alias.e164Number, dma.ip)

        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(vmr, CallType.SIP, 2048)
        groupSeries.placeCall(vmr, CallType.H323, 2048)
        pauseTest(10)


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
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--")

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
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")

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
        groupSeries.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(10)

        where:
        vmr    | pool_order               | _
        "8910" | "RMX1800_10.220.206.102" | _
        "8911" | "RMX2000_172.21.113.222" | _
    }

    @Unroll
    def "Verify mooncake can join #pool_order vmr conference via H323 and GS via SIP"() {
        setup:
        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, null, null)
        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        groupSeries.registerSip(gs_alias.sipUri, "polycom.com", "", dma.ip)

        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(vmr, CallType.H323, 2048)
        groupSeries.placeCall(vmr, CallType.SIP, 2048)
        pauseTest(10)


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
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--")

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
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")

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
        groupSeries.hangUp()
        dma.deleteVmr(vmr)
        pauseTest(10)

        where:
        vmr    | pool_order               | _
        "8910" | "RMX1800_10.220.206.102" | _
        "8911" | "RMX2000_172.21.113.222" | _
    }
}
