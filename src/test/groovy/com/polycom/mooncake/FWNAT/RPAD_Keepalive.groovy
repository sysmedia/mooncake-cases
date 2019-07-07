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
 * Created by dyuan on 5/29/2019
 *
 * # External Oculus and internal GS P2P SIP call.
 * # 1. Send content/FECC keep alive after idle 5 minutes in a p2p call.
 * # 2. Receive content/FECC keep alive after idle 5 minutes in a p2p call.
 * # RPAD environment
 */
class RPAD_Keepalive extends MoonCakeSystemTestSpec {

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
        mc_alias = generateDialString(moonCake)
        gs_alias = generateDialString(groupSeries)
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    def "Do sip p2p call and keep alive 5 minutes then send content and FECC"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")
        groupSeries.registerSip(gs_alias.sipUri, "polycom.com", "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        logger.info("===============Start SIP Call with call rate " + 2048 + "===============")
        moonCake.placeCall(gs_alias.sipUri, CallType.SIP, 2048)
        pauseTest(5)


        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        logger.info("===============Successfully start SIP Call with call rate " + 2048 + "===============")

        when: "Pause minutes "
        pauseTest(300)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        when: "Push content and send FECC on the mooncake"
        moonCake.sendFECC("RIGHT")
        pauseTest(5)
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:true")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:true")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:true")

        when: "Push content on the group series"
        pauseTest(60)
        groupSeries.sendFECC("right")
        pauseTest(5)
        groupSeries.playHdmiContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }

    def "Do h323 p2p call and keep alive 5 minutes then send content and FECC"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()


        when: "Set MoonCake SIP registered and Group Series SIP registered"
        moonCake.registerGk(true, false, rpad_ip, mc_alias.h323Name, mc_alias.e164Number, "", "")
        groupSeries.registerGk(gs_alias.h323Name, gs_alias.e164Number, dma.ip)
        pauseTest(5)
        logger.info("===============Start h323 Call with call rate " + 2048 + "===============")
        moonCake.placeCall(gs_alias.h323Name, CallType.H323, 2048)
        pauseTest(5)


        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        logger.info("===============Successfully start h323 Call with call rate " + 2048 + "===============")

        when: "Pause minutes "
        pauseTest(60)

        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")

        when: "Push content and send FECC on the mooncake"
        moonCake.sendFECC("RIGHT")
        pauseTest(5)
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:true")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:true")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:true")
        verifyMediaStatistics(MediaChannelType.CVTX, "--:--:--:--:0:true")

        when: "Push content on the group series"
        pauseTest(60)
        groupSeries.sendFECC("right")
        pauseTest(5)
        groupSeries.playHdmiContent()
        pauseTest(5)

        then: "Verify the media statistics during MoonCake show content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "--:--:--:--:0:--")

        cleanup:
        moonCake.hangUp()
        pauseTest(5)
    }
}
