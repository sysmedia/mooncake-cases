package com.polycom.mooncake.DialString

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.mediastatistics.CallType
import com.polycom.mooncake.MoonCakeSystemTestSpec
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 4/22/2019
 */
class DialString_Negative_H323 extends MoonCakeSystemTestSpec{
    @Shared
    Dma dma

    @Shared
    String h323Name = "automooncake"

    @Shared
    String e164Num = "84381100"

    def setupSpec() {
        dma = testContext.bookSut(Dma.class, keyword)
        moonCake.init()
        moonCake.setEncryption("no")
        moonCake.enableH323()
    }

    def cleanupSpec() {
        testContext.releaseSut(dma)
        //clean moonCake GK register
        moonCake.registerGk(false, false, "", "", "", "", "")
    }

    //Create a list of negative dial string
    @Shared
    def dialString = ["999.999.999.999", "`@#^={}|;\$'~!?%&*()_ +]-.:<>/\\,中", " ", "10.0.0.1", "test", \
                            "test@172.0.1.22", "10.251.0.33##1234"]
    @Unroll
    def "Test dial out when H.323 not registered"(String dialStr){
        when:
        //make sure moonCake's H323 was not registered
        moonCake.registerGk(false, false, "", "", "", "", "")
        pauseTest(3)

        then:
        logger.info("=====MoonCake place call to<"+ dialStr + ">=====")
        moonCake.placeCall(dialStr, CallType.H323, 512)
        pauseTest(10)
        //make sure call was end success
        moonCake.hangUp()
        pauseTest(5)
        logger.info("=====Call hang up success!=====")

        where:
        [dialStr] << getDialStr(dialString + moonCake.getIp())
    }

    //Create a list of negative dial string
    @Shared
    def dialString1 = ["234.234.234.234", "test123", " ", e164Num]
    @Unroll
    def "Test dial out when H.323 registered"(String dialStr){
        when:
        //moonCake register H323
        moonCake.registerGk(true, false, dma.ip, h323Name, e164Num, "", "")
        pauseTest(3)

        then:
        logger.info("=====MoonCake place call to<"+ dialStr + ">=====")
        moonCake.placeCall(dialStr, CallType.H323, 512)
        pauseTest(10)
        //make sure call was end success
        moonCake.hangUp()
        pauseTest(5)
        logger.info("=====Call hang up success!=====")

        where:
        [dialStr] << getDialStr(dialString1)
    }

    //get DialStr
    def getDialStr(List str){
        def rtn=[]
        str.each { rtn << [it] }
        return rtn
    }
}
