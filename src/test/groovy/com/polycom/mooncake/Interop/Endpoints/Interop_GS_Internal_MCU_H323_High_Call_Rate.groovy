package com.polycom.mooncake.Interop.Endpoints

import com.polycom.auto.resources.enums.SipTransportProtocol
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.RpdWin
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared

/**
 * Created by qxu on 4/30/2019
 */
class Interop_GS_Internal_MCU_H323_High_Call_Rate extends MoonCakeSystemTestSpec{
    @Shared
    GroupSeries groupSeries
    @Shared
    RpdWin rpdWin
    @Shared
    Dma dma

    @Shared
    String moonH323UserName = "moonauto"
    @Shared
    String moon_e164Num = "843811006"
    @Shared
    String gsSipUserName = "gsSipauto"
    @Shared
    String gsH323Name = "gsH323auto"
    @Shared
    String gs_e164Num = "843811001"
    @Shared
    String rpdSipUserName = "rdpSipautoTest"
    @Shared
    String rpdH323UsrName = "rpd323"
    @Shared
    String rpdExtNum = "105452323"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        rpdWin = testContext.bookSut(RpdWin.class, keyword)
        dma = testContext.bookSut(Dma.class, keyword)
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(rpdWin)
        testContext.releaseSut(dma)
    }

    def "All EP H.323 + All EP the highest call rate + AES on"(){
        setup: "make sure all endpoint was not in call"
        hangUpAll()
        moonCake.updateCallSettings(4096, "on", true, false, true)
        groupSeries.setEncryption("yes")

        when: "Endpoints register"
        allEndpointsRegister()

        then: "rpd place H323, moonCale place H323 with GS internal MCU"
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(gs_e164Num, CallType.H323, 2048)
        }
        retry(times: 3, delay: 5){
            moonCake.placeCall(gs_e164Num, CallType.H323, 4096)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "moonCake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        then: "moonCake stop content"
        moonCake.stopContent()
        pauseTest(3)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "rpd push content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        then: "GS push content"
        groupSeries.playContent(2)
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        cleanup: "hang up all Endpoints"
        hangUpAll()
        groupSeries.setEncryption("no")
    }

    def "SUT H323 1024k + Another SUT SIP 1024k"(){
        setup: "make sure all endpoint was not in call"
        hangUpAll()
        moonCake.updateCallSettings(1024, "off", true, false, true)

        when: "Endpoints register"
        allEndpointsRegister()

        then: "moonCale place H323, rpd place SIP with GS internal MCU"
        retry(times: 3, delay: 5){
            moonCake.placeCall(gs_e164Num, CallType.H323, 1024)
        }
        retry(times: 3, delay: 5) {
            rpdWin.placeCall(gsSipUserName, CallType.SIP, 1024)
        }

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        then: "GS push content"
        groupSeries.playContent(2)
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        then: "moonCake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264High:--:--:--:--:--")

        then: "rpd push content"
        retry(times: 3, delay: 5) {
            rpdWin.pushContent()
        }
        pauseTest(5)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264High:--:--:--:--:--")

        then: "Rpd stop content"
        rpdWin.stopContent()
        pauseTest(3)

        then: "Verify the media statistics during the call"
        verifyMediaStatistics(MediaChannelType.ARX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ATX, "SirenLPR:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "H.264High:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "H.264High:--:--:--:--:--")

        cleanup: "hang up all Endpoints"
        hangUpAll()
    }


    def allEndpointsRegister(){
        moonCake.enableH323()
        moonCake.registerGk(true, false, dma.ip, moonH323UserName, moon_e164Num, "", "")
        groupSeries.enableSIP()
        groupSeries.registerSip(gsSipUserName, centralDomain, "", dma.ip, SipTransportProtocol.SIP_TRANSPORT_PROTOCOL_TLS)
        groupSeries.enableH323()
        groupSeries.registerGk(gsH323Name, gs_e164Num, dma.ip)
        rpdWin.enableSip()
        rpdWin.registersip(dma.ip, rpdSipUserName, "", "", "", "TCP")
        rpdWin.enableH323()
        rpdWin.registerH323(dma.ip, rpdH323UsrName, rpdExtNum)
        pauseTest(3)
    }

    def hangUpAll(){
        moonCake.hangUp()
        rpdWin.hangUp()
        //groupSeries.hangUp()
    }

}
