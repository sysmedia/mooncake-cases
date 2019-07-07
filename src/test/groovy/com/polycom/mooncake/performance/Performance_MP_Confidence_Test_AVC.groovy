package com.polycom.mooncake.performance

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.*
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.test.asserts.PctcAssertions
import com.polycom.honeycomb.test.performance.PerfDataProbe
import com.polycom.honeycomb.test.performance.Performance
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by taochen on 2019-05-28.
 *
 * Confidence test for MP AVC call for H.323 and SIP
 * Monitor CPU, Memory and temperature during the test
 * SUT and EP dial in conference with H.323 or SIP randomly
 * SUT and EP dial in conference with random call rate
 * SUT mute/unmute audio randomly during call
 * SUT and EP send content randomly during call
 * SUT and EP hangup call randomly, then re-dial in
 */
@Performance(runTimes = 250)
class Performance_MP_Confidence_Test_AVC extends MoonCakeSystemTestSpec {
    @Shared
    PerfDataProbe probe

    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    RpdWin rpdWin

    @Shared
    MoonCake moonCake2

    @Shared
    String mc_sipUri

    @Shared
    String gs_h323Name

    @Shared
    String gs_e164Num

    @Shared
    String gs_sipUri

    @Shared
    String mc2_h323Name

    @Shared
    String mc2_e164Num

    @Shared
    String rpd_h323Name

    @Shared
    String rpd_e164Num

    @Shared
    String rpd_sipUri

    @Shared
    String vmr = "1915"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        moonCake2 = testContext.bookSut(MoonCake.class, keyword, "backup")
        dma = testContext.bookSut(Dma.class, keyword)

        groupSeries.init()
        rpdWin.init()
        moonCake2.init()

        groupSeries.setEncryption("yes")
        groupSeries.enableH323()
        rpdWin.enableH323()
        moonCake.enableSIP()
        moonCake.setEncryption("yes")
        moonCake2.enableH323()
        moonCake2.setEncryption("yes")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, poolOrder, dma.domain, dma.username, null, null)

        def gsDialString = generateDialString(groupSeries)
        def rpdDialString = generateDialString(rpdWin)
        def mc2DialString = generateDialString(moonCake2)
        mc_sipUri = generateDialString(moonCake).sipUri
        gs_h323Name = gsDialString.h323Name
        gs_e164Num = gsDialString.e164Number
        gs_sipUri = gsDialString.sipUri
        rpd_h323Name = rpdDialString.h323Name
        rpd_e164Num = rpdDialString.e164Number
        rpd_sipUri = rpdDialString.sipUri
        mc2_h323Name = mc2DialString.h323Name
        mc2_e164Num = mc2DialString.e164Number

        probe = testContext.addPerfDataProbe(moonCake, 60000)
    }

    def cleanupSpec() {
        dma.deleteVmr(vmr)
        dma.deleteConferenceTemplateByName(confTmpl)
        moonCake.init()
        moonCake2.init()
        testContext.releaseSut(moonCake2)
        testContext.releaseSut(dma)
        testContext.releaseSut(rpdWin)
        testContext.releaseSut(groupSeries)
    }

    def "AVC MP Confidence Test"() {
        CallType moonCakeCallType = generateRandomCallType()
        int moonCakeCallRate = generateRandomCallRate()
        CallType gsCallType = generateRandomCallType()
        int gsCallRate = generateRandomCallRate()
        CallType rpdCallType = generateRandomCallType()
        int rpdCallRate = generateRandomCallRate()

        setup:
        moonCake.hangUp()
        moonCake2.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()

        when: "Make the endpoints register the GK and SIP server"
        moonCake.registerSip("tls", true, "", dma.ip, "", mc_sipUri, "")
        moonCake2.registerGk(true, false, dma.ip, mc2_h323Name, mc2_e164Num, "", "")
        groupSeries.registerGk(gs_h323Name, gs_e164Num, dma.ip)
        groupSeries.registerSip(gs_sipUri, centralDomain, "", dma.ip)
        rpdWin.registerH323(dma.ip, rpd_h323Name, rpd_e164Num)
        rpdWin.registersip(dma.ip, rpd_sipUri, "", "", "", "TLS")

        then: "Make the endpoints please call to join the conference with their call type and rate randomly choose"
        retry(times: 3, delay: 5) {
            if (moonCakeCallType == CallType.SIP) {
                moonCake.placeCall(vmr, CallType.SIP, moonCakeCallRate)
                logger.info("MoonCake place SIP call with rate " + moonCakeCallRate)
            } else {
                moonCake2.placeCall(vmr, CallType.H323, moonCakeCallRate)
                logger.info("MoonCake place H323 call with rate " + moonCakeCallRate)
            }

            groupSeries.placeCall(vmr, gsCallType, gsCallRate)
            logger.info("GroupSeries place " + gsCallType + " call with rate " + gsCallRate)

            rpdWin.placeCall(vmr, rpdCallType, rpdCallRate)
            logger.info("RPD place " + rpdCallType + " call with rate " + rpdCallRate)
        }
        pauseTest(15)

        then: "Check the media statistics on all endpoints"
        if (moonCake.callStatus == "CONNECT") {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--")
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--")
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")
        }

        if (moonCake2.callStatus == "CONNECT") {
            verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:0:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:--:--", moonCake2)
            verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--", moonCake2)
        }

        retry(times: 5, delay: 5) {
            PctcAssertions.assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ATX)
            PctcAssertions.assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.ARX)
            PctcAssertions.assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVTX)
            PctcAssertions.assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.PVRX)
            PctcAssertions.assertThat(groupSeries).hasLiveMediaChannel(MediaChannelType.CVTX)

            PctcAssertions.assertThat(rpdWin).hasLiveMediaChannel(MediaChannelType.ATX)
            PctcAssertions.assertThat(rpdWin).hasLiveMediaChannel(MediaChannelType.ARX)
            PctcAssertions.assertThat(rpdWin).hasLiveMediaChannel(MediaChannelType.PVTX)
            PctcAssertions.assertThat(rpdWin).hasLiveMediaChannel(MediaChannelType.PVRX)
            PctcAssertions.assertThat(rpdWin).hasLiveMediaChannel(MediaChannelType.CVTX)
        }

        then: "Randomly mute video on endpoint during the call"
        Boolean gsMute = randomMute()
        Boolean mcMute = randomMute()
        Boolean rpdMute = randomMute()
        groupSeries.setVideoMuted(gsMute)
        if (moonCake.callStatus == "CONNECT") {
            moonCake.muteVideo(mcMute)
        }

        if (moonCake2.callStatus == "CONNECT") {
            moonCake2.muteVideo(mcMute)
        }

        if (rpdMute) {
            rpdWin.muteAudio()
        } else {
            rpdWin.unMuteAudio()
        }
        logger.info("Set the GroupSeries video as " + gsMute)
        logger.info("Set the MoonCake video as " + mcMute)
        logger.info("Set the RPD audio as " + rpdMute)

        then: "Hold on the call with random period and do some operation randomly"
        retry(times: getRandomIntegerNumberInRange(12, 36), delay: 300) {
            Endpoint selectedEp = randomSelectEndpoint(moonCake, moonCake2, groupSeries, rpdWin)
            if (selectedEp instanceof MoonCake) {
                if ((MoonCake) selectedEp.callStatus == "CONNECT") {
                    (MoonCake) selectedEp.pushContent()
                    pauseTest(10)
                    PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ATX)
                    PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ARX)
                    PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVTX)
                    PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVRX)
                    PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.CVTX)
                    (MoonCake) selectedEp.stopContent()
                }
            } else if (selectedEp instanceof GroupSeries) {
                (GroupSeries) selectedEp.playHdmiContent()
                pauseTest(10)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ATX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ARX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVTX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVRX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.CVTX)
                (GroupSeries) selectedEp.stopContent()
            } else if (selectedEp instanceof RpdWin) {
                (RpdWin) selectedEp.pushContent()
                pauseTest(10)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ATX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.ARX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVTX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.PVRX)
                PctcAssertions.assertThat(selectedEp).hasLiveMediaChannel(MediaChannelType.CVTX)
                (RpdWin) selectedEp.stopContent()
            }
        }

        cleanup:
        moonCake.hangUp()
        moonCake2.hangUp()
        groupSeries.hangUp()
        rpdWin.hangUp()
        pauseTest(5)
    }

    def generateRandomCallRate() {
        int rtn
        int opt = getRandomIntegerNumberInRange(1, 6)
        switch (opt) {
            case 1:
                rtn = 512
                break
            case 2:
                rtn = 1024
                break
            case 3:
                rtn = 1536
                break
            case 4:
                rtn = 2048
                break
            case 5:
                rtn = 3072
                break
            case 6:
                rtn = 4096
                break
        }
        return rtn
    }

    def generateRandomCallType() {
        int opt = getRandomIntegerNumberInRange(1, 2)
        CallType rtn = (opt == 1 ? CallType.SIP : CallType.H323)
        return rtn
    }

    def randomMute() {
        int opt = getRandomIntegerNumberInRange(1, 2)
        Boolean rtn = (opt == 1 ? true : false)
        return rtn
    }

    def randomSelectEndpoint(Endpoint... endpoints) {
        int opt = getRandomIntegerNumberInRange(1, endpoints.length)
        Endpoint rtn = endpoints[opt - 1]
        return rtn
    }
}
