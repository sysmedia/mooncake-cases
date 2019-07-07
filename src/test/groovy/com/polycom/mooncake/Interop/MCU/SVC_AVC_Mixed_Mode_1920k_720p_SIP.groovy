package com.polycom.mooncake.Interop.MCU

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Gary Wang on 2019-06-14.
 *
 Verify that video quality is good for AVC and SVC video transcoding in AVC/SVC mixed conference with 1920k 720p enabled
 Test AVC 1080p EP + SVC 720p SUT
 Test MCU can decode AVC 1080p to SVC 720p/360p/180p
 Test MCU can decode AVC 720p to SVC 720p/360p/180p
 Test MCU can decode AVC 360p to SVC 360p/180p
 Test MCU can decode AVC 180p to SVC 180p
 Test MCU can decode AVC other resolution to SVC 360p/180p
 Test MCU can decode SVC 720p/360p/180p to AVC 720p/360p/180p
 Test 1080p/720p content sending and receiving
 ??Test that MCU can transcode 1080p with different video rate and frame rate (7.5/15/30)

 *
 * Enable this feature by MCU System Flag
 * - ENABLE_HIGH_VIDEO_RES_ AVC_TO_SVC_IN_MIXED_MODE: YES
 * - ENABLE_HIGH_VIDEO_RES_ SVC_TO_AVC_IN_MIXED_MODE: YES
 * - ENABLE_1080_SVC: YES
 */
class SVC_AVC_Mixed_Mode_1920k_720p_SIP extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String vmr = "1920100"

    @Shared
    def gs_h323name = "Auto_GS550"

    @Shared
    def gs_e164 = "1721126888"

    @Shared
    String gs_sip_username = "GS550Sip"

    @Shared
    String h323Name = "automooncake4096"

    @Shared
    String e164Num = "843811004096"

    @Shared
    String sipUri = "mooncake4096"

    @Shared
    def callRateLists = [2048 , 512]

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, "GS700")
        groupSeries.init()
        groupSeries.setEncryption("yes")

        moonCake.updateCallSettings(2048, "auto", true, false, true);
        dma = testContext.bookSut(Dma.class, keyword)
        def dialString = generateDialString(moonCake)
        sipUri = dialString.sipUri
        h323Name = dialString.h323Name
        e164Num = dialString.e164Number
        def dialString2 = generateDialString(groupSeries)
        gs_h323name = dialString2.h323Name
        gs_e164 = dialString2.e164Number
        gs_sip_username = dialString2.sipUri
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "", dma.ip, sipUri, sipUri, "")

        groupSeries.enableH323()
        groupSeries.registerGk(gs_h323name, gs_e164, dma.ip)
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        testContext.releaseSut(groupSeries)
    }

    @Unroll
    def "Verify MoonCake dial in conference with different resolution for AVC"(String dialString,
                                                                                              int callRate) {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Place call on the mooncake with call rate 4096Kbps"
        logger.info("===============Start H.323 Call with call rate " + callRate + "===============")
        retry(times: 3, delay: 5) {
            groupSeries.placeCall(dialString,CallType.H323,callRate)
            pauseTest(3)
        }

        for( int callrate in callRateLists) {
            retry(times: 3, delay: 5) {
                moonCake.placeCall(dialString, CallType.SIP, callrate)
                pauseTest(3)
            }

            then: "Verify the media statistics when no content during the call"
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

            then: "Push content on the GS"
            groupSeries.playHdmiContent()

            then: "Verify the media statistics during the call"
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:--:--")
            logger.info("===============Successfully start H323 Call with call rate " + callRate + "===============")

            then:"Stop play content, and moonCake push content , verify the media statistics during the call"
            groupSeries.stopContent()
            moonCake.pushContent()
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:--:--")

            then:"moonCake hangup"
            moonCake.hangUp()
            pauseTest(5)
        }

        then:"Group hangup"
        groupSeries.hangUp()
        pauseTest(3)

        where:
        [dialString, callRate] << getTestData_1()
    }

    /**
     * Dial String for H323 call
     *
     * @return
     */
    def getTestData_1() {
        def rtn = []
        rtn << [vmr, 2048]
        rtn << [vmr, 1024]
        rtn << [vmr, 512]
        return rtn
    }


}
