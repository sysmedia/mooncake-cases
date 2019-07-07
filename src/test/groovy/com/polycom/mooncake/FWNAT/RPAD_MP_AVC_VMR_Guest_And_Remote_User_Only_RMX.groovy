package com.polycom.mooncake.FWNAT

import com.polycom.api.rest.plcm_conference_template_v7.ConferenceCodecSupport
import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.RpdMac
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by dyuan on 6/12/2019
 * # Test the AVC VMR by only guest and remote user
 * # Description: RPAD environment: SIP&H323, AVC VMR, only external EP, no internal EP
 */
class RPAD_MP_AVC_VMR_Guest_And_Remote_User_Only_RMX extends MoonCakeSystemTestSpec {
    @Shared
    RpdMac rpdMac

    @Shared
    Dma dma

    @Shared
    def mc_alias

    @Shared
    def rpd_alias

    def setupSpec() {
        rpad_ip = testContext.getValue("Rpad_ip")

        moonCake.setEncryption("auto")

        dma = testContext.bookSut(Dma.class, "FWNAT")

        //Create AVC only conference template
        dma.createConferenceTemplate(confTmpl, "AVC only template", "2048", ConferenceCodecSupport.AVC)

        rpdMac = testContext.bookSut(RpdMac.class, "FWNAT")

        mc_alias = generateDialString(moonCake)
        rpd_alias = generateDialString(rpdMac)
    }

    def cleanupSpec() {
        dma.deleteConferenceTemplateByName(confTmpl)

        testContext.releaseSut(dma)
        testContext.releaseSut(rpdMac)
    }

    @Unroll
    def "Verify external rpadmac sip call and mooncake h323 guest call VMR"() {
        setup:
        moonCake.enableH323()
        moonCake.registerGk(false, false, "", "", "", "", "")
        rpdMac.registersip(rpad_ip, rpd_alias.sipUri, "polycom.com", "", "", "TLS")

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, null, null)
        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(vmr + "@" + rpad_ip, CallType.H323, 2048)
        pauseTest(5)


        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")


        when: "rpdMac call in"
        rpdMac.placeCall(vmr, CallType.SIP, 2048)
        pauseTest(5)

        then: "Verify the rpdMac's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)

        when: "Mooncake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264*:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--", rpdMac)

        when: "rpdMac push content"
        rpdMac.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--", rpdMac)

        cleanup:
        dma.deleteVmr(vmr)
        moonCake.hangUp()
        rpdMac.hangUp()
        pauseTest(5)

        where:
        vmr    | pool_order
        "9771" | "RMX1800_10.220.206.102"
        "9772" | "RMX2000_172.21.113.222"
    }

    @Unroll
    def "Verify external rpadmac h323 call and mooncake siptls call VMR"() {
        setup:
        moonCake.enableSIP()
        moonCake.registerSip("TLS", true, "polycom.com", rpad_ip, "", mc_alias.sipUri, "")
        rpdMac.registerH323(rpad_ip, rpd_alias.h323Name, rpd_alias.e164Number)

        //Create VMR on DMA
        dma.createVmr(vmr, confTmpl, pool_order, dma.domain, dma.username, null, null)
        pauseTest(5)


        when: "Mooncake call in"
        moonCake.placeCall(vmr, CallType.SIP, 2048)
        pauseTest(5)


        then: "Verify the MoonCake's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")


        when: "rpdMac call in"
        rpdMac.placeCall(vmr, CallType.H323, 2048)
        pauseTest(5)

        then: "Verify the rpdMac's media statistics during the call"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)

        when: "Mooncake push content"
        moonCake.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264*:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264*:--:--:--:0:--", rpdMac)

        when: "rpdMac push content"
        rpdMac.pushContent()
        pauseTest(5)

        then: "Verify the MoonCake's media statistics during push content"
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--")
        verifyMediaStatistics(MediaChannelType.CVRX, "H.264:--:--:--:0:--")
        verifyMediaStatistics(MediaChannelType.PVRX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.PVTX, "--:--:--:--:0:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ARX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.ATX, "--:--:--:--:--:--", rpdMac)
        verifyMediaStatistics(MediaChannelType.CVTX, "H.264:--:--:--:0:--", rpdMac)

        cleanup:
        dma.deleteVmr(vmr)
        moonCake.hangUp()
        rpdMac.hangUp()
        pauseTest(5)

        where:
        vmr    | pool_order
        "9771" | "RMX1800_10.220.206.102"
        "9772" | "RMX2000_172.21.113.222"
    }
}
