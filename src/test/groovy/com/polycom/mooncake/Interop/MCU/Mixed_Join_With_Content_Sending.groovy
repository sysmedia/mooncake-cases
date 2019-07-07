package com.polycom.mooncake.Interop.MCU

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.Mcu
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Gary Wang on 2019-06-06
 *
 *This case is to verify MoonCake and other endpoints join Mixed conference while there is content sharing
 *FEs join conference and send content, then SUT joins conference
 *
 * Check audio, video,content
 * Test with H323 and SIP TCP
 */
class Mixed_Join_With_Content_Sending extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    Mcu mcu

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "409699"

    @Shared
    def ep_h323name = "Auto_GS550"

    @Shared
    def ep_e164 = "1721126888"

    @Shared
    String ep_sip_username = "GS550Sip"
    @Shared
    String h323Name = "automooncake4096"

    @Shared
    String e164Num = "843811004096"

    @Shared
    String sipUri = "mooncake4096"

    def setupSpec() {

        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()

        moonCake = testContext.bookSut(MoonCake.class,keyword)
        moonCake.updateCallSettings(4096, "off", true, false, true);

        dma = testContext.bookSut(Dma.class, keyword)
        mcu = testContext.bookSut(Mcu.class, keyword)
        def dialString = generateDialString(moonCake)
        sipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number
        def dialString2 = generateDialString(groupSeries)
        ep_h323name = dialString2.h323Name
        ep_e164 = dialString2.e164Number
        ep_sip_username = dialString2.sipUri

        groupSeries.enableH323()
        groupSeries.enableSIP()
        groupSeries.registerGk(ep_h323name,ep_e164,dma.ip)
        groupSeries.registerSip(ep_sip_username, centralDomain, "",dma.ip,SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TCP)

    }

    def cleanupSpec() {

        testContext.releaseSut(dma)
        testContext.releaseSut(mcu)
        testContext.releaseSut(groupSeries)

    }
    @Unroll
    def "Verify FEs joins conference by H.323 and sends content, then MoonCake join conference" (
            String dialString, CallType callType, int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.enableH323()

        when: "Set the MoonCake and GS with registering the DMA via H323"
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        pauseTest(10)
        then: "Place call on the FEs with call rate 4096Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(dialString, callType, callRate)
            pauseTest(3)
        }

        then: "Verify the media statistics when no content during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)

        then: "Push content on the Group"
        groupSeries.playHdmiContent()

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", groupSeries)
        logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

        then:"MoonCake join conference ,verify the media statistics during the call"
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, callType, callRate)
        }
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", groupSeries)
        captureScreenShot(moonCake)

        then: "MoonCake Send content"
        moonCake.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", groupSeries)
        captureScreenShot(moonCake)

        then: "MoonCake hangup and Group continue to push content"
        moonCake.hangUp()
        groupSeries.playHdmiContent()

        then: "MoonCake register to SIP and join conference"
        pauseTest(3)
        moonCake.enableSIP()
        pauseTest(3)
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        pauseTest(30)
        retry(times: 3, delay: 5) {
            moonCake.placeCall(dialString, CallType.SIP, callRate)
            pauseTest(3)
        }

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--", groupSeries)
        captureScreenShot(moonCake)

        then: "MoonCake Send content"
        moonCake.pushContent()
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:1920x1080:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264-HP:1080p:--:--:--:--", groupSeries)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--", groupSeries)
        captureScreenShot(moonCake)

        cleanup:
        moonCake.hangUp()
        groupSeries.hangUp()
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(10)

        where:
        [dialString, callType, callRate] << getTestData_1()
    }

      /**
     * Dial String for H323 and SIP call
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        rtn << [vmr, CallType.H323, 4096]
        rtn << [vmr, CallType.SIP, 4096]
        return rtn
    }

}
