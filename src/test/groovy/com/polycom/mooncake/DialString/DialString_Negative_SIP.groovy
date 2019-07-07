package com.polycom.mooncake.DialString

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 4/22/2019
 */
class DialString_Negative_SIP extends MoonCakeSystemTestSpec{
    @Shared
    GroupSeries groupSeries
    @Shared
    Dma dma
    @Shared
    def mc_sip_username = "1025090"
    @Shared
    def gs_sip_username = "1025091"
    @Shared
    def gs_sip_domain = "sqa.org"

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")
        groupSeries.enableSIP()
        dma = testContext.bookSut(Dma.class, keyword)
        moonCake.init()
        moonCake.setEncryption("no")
        moonCake.enableSIP()
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        //clean moonCake sip register
        moonCake.registerSip("",false, "", "","","","")
        //groupSeries.registerSip("","","","")
    }

    //Create a list of negative dial string
    @Shared
    def dialString1 = ["999.999.999.999", "`@#^={}|;\$'~!?%&*()_ +]-.:<>/\\,中", "sips:123", "10.0.0.0", "test", \
                        "test@172.0.1.22", "10.251.0.33##123", ""]
    @Unroll
    def "Test dial out with SIP not registered"(String dialStr){
        when:
        //make sure MoonCake was under SIP not registered status
        moonCake.registerSip("",false, "", "","","","")
        pauseTest(3)

        then:
        logger.info("=====MoonCake place call to<"+ dialStr + ">=====")
        moonCake.placeCall(dialStr, CallType.SIP, 512)
        pauseTest(10)
        //make sure call was end success
        moonCake.hangUp()
        pauseTest(5)
        logger.info("=====Call hang up success!=====")

        where:
        [dialStr] << getDialStr(dialString1 + moonCake.getIp())
    }

    //Create a list of negative dial string
    @Shared
    def dialString2 = ["234.234.234.234", "test123", " "]
    @Unroll
    def "Test dial out with SIP registered"(String dialStr){
        when:
        //moonCake register to SIP server
        moonCake.registerSip("UDP", true, "", dma.ip, mc_sip_username, "", "")
        //GS register to SIP server
        groupSeries.registerSip(gs_sip_username, gs_sip_domain, "", dma.ip) //no protocol, default using UDP
        pauseTest(3)
        //def addStr = "sips:"+groupSeries.getUsername()

        then:
        logger.info("=====MoonCake place call to<"+ dialStr + ">=====")
        moonCake.placeCall(dialStr, CallType.SIP, 512)
        pauseTest(10)
        //make sure call was end success
        moonCake.hangUp()
        pauseTest(5)
        logger.info("=====Call hang up success!=====")

        where:
        [dialStr] << getDialStr1(dialString2)
       }

    //get DialStr
    def getDialStr(List str){
        def rtn=[]
        str.each { rtn << [it] }
        return rtn
    }
    //get DialStr1
    def getDialStr1(List str){
        def rtn = []
        //add moonCake and GS's username to the dial list
        def addStr = "sips:"+groupSeries.getUsername()
        str = str + moonCake.getUsername() + addStr
        str.each { rtn << [it]}
        return rtn
    }

}
