package com.polycom.mooncake.WebUITests

import com.polycom.honeycomb.Endpoint
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.WebUiTestSpec
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import geb.driver.CachingDriverFactory
import spock.lang.Shared

import java.text.SimpleDateFormat

class MoonCakeUITestSpec extends WebUiTestSpec{
    @Shared
    MoonCake moonCake

    @Shared
    String keyword = System.getProperty("KEYWORD")

    @Shared
    String environment = System.getProperty("geb.env")

    def setupSpec() {
        if (keyword == null) {
            keyword = "WebUI"
        }

        //Set local environment if needed
        if (environment == null) {
            //System.setProperty("geb.env","ie")
            System.setProperty("geb.env","chrome")
            //System.setProperty("geb.env", "firefox")
        }
        moonCake = testContext.bookSut(MoonCake.class, keyword)
        //Set the base URL
        System.setProperty("geb.build.baseUrl", "https://" + moonCake.getIp() + ":"+ moonCake.getPort())
        browser.driver.manage().window().maximize()

        DeviceStatusPage deviceStatusPage = browser.via DeviceStatusPage
        //We need click override link in IE11 first
        if(browser.driver.toString().contains("InternetExplorerDriver")) {
            browser.driver.get("javascript:document.getElementById('overridelink').click()")
        }
        deviceStatusPage = browser.at DeviceStatusPage
    }

    def cleanupSpec() {
        //Reset Browser and Close Web Driver
        resetBrowser()
        CachingDriverFactory.clearCacheAndQuitDriver()

        testContext.releaseSut(moonCake)
    }

    /**
     * Convert csv file to map
     * @param filePath The path of target file
     * @param separator The separator in csv file
     * @return map
     */
    def convertCsvFileToMap(String filePath, String separator) {
        File csvFile = new File(filePath)
        def map = [:]
        csvFile.eachLine { line->
            def parts = line.split(separator)
            map.put(parts[0],parts[1])
        }
        return map
    }

    /**
     * Generate the dial string for specified endpoint
     *
     * @param ep The endpoint
     * @return The dial string map
     */
    def generateDialString(Endpoint ep) {
        sleep(1) //in case some tests need to book several same type devices
        def epType = ep.getClass().getSimpleName()
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
        def h323Name = "autoH323" + epType + timeStamp
        def e164Number = timeStamp.substring(2)
        def sipUri = "autoSip" + epType + timeStamp
        def rtn = [h323Name: h323Name, e164Number: e164Number, sipUri: sipUri]
        return rtn
    }
}
