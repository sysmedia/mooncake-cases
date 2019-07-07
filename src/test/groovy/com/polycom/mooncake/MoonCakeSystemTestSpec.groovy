package com.polycom.mooncake

import com.polycom.honeycomb.Endpoint
import com.polycom.honeycomb.MoonCake
import com.polycom.honeycomb.SystemTestSpec
import com.polycom.honeycomb.ftp.FtpClient
import com.polycom.honeycomb.mediastatistics.MediaChannelStatistics
import com.polycom.honeycomb.mediastatistics.MediaChannelType
import com.polycom.honeycomb.mediastatistics.MediaStatistics
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import spock.lang.Shared

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

/**
 * Created by taochen on 2019-03-29.
 */
class MoonCakeSystemTestSpec extends SystemTestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(this.getClass())

    @Shared
    MoonCake moonCake

    @Shared
    String keyword = System.getProperty("KEYWORD")

    @Shared
    String SAT = "SAT"

    @Shared
    String centralDomain = "sqa.org"

    @Shared
    String centralDns = "172.21.115.98"

    @Shared
    String mcuPrefix = "20321599"

    @Shared
    String mcuSignalIP

    @Shared
    String mcuConfProfile = "SipReg"

    @Shared
    String entryQueue = "666778"

    @Shared
    String confTmpl = "Auto_AVC_Only_Conference_Template"

    @Shared
    String poolOrder = "Factory Pool Order"

    @Shared
    String poolOrderRMX4000NGB = "RMX4000-NGB"

    @Shared
    String poolOrderRMX1800 = "RMX1800"

    @Shared
    String poolOrderRMXVE = "RMXVE"

    @Shared
    String rpad_ip

    @Shared
    def callRateList = [256, 384, 512, 768, 1024, 2048, 4096] as int[]

    @Shared
    String vmrAVCAuto = "1024301"

    @Shared
    String vmrAVCOff = "1024302"

    @Shared
    String veqAuto = "1024401"

    @Shared
    String veqOff = "1024402"

    @Shared
    String callTmplAVCAuto = "avctmplauto"

    @Shared
    String callTmplAVCOff = "avctmploff"

    @Shared
    String avcAutoEqName = "automation_AVC_Auto"

    @Shared
    String avcOffEqName = "automation_AVC_Off"

    def setupSpec() {
        if (keyword == null) {
            keyword = SAT
        }
        moonCake = testContext.bookSut(MoonCake.class, keyword)
        moonCake.init()
    }

    def cleanupSpec() {
        moonCake.reboot()
        pauseTest(300)
        testContext.releaseSut(moonCake)
    }

    /**
     * Verify the media statistics by specified media channel
     *
     * @param channelType The media channel
     * @param expectedValueList The expected channel media value list. For example
     *                          ATX "protocol:resolution:rate:frameRate:rateUsed:encryption".
     *                          If you do not want to check specific value, just put "--" instead. ATX "--:--:--:--:--:--".
     * @param eps The specified endpoints need to verify their media statistics
     * @return Assert true or false
     */
    def verifyMediaStatistics(MediaChannelType channelType, String expectedValueList, Endpoint... eps) {
        retry(times: 5, delay: 5) {
            // parse the input expected values on the channel
            String[] expectedValues = expectedValueList.split(":")
            String expectedProtocol = expectedValues[0]
            String expectedResolution = expectedValues[1]
            String expectedRate = expectedValues[2]
            String expectedFrameRate = expectedValues[3]
            String expectedRateUsed = expectedValues[4]
            String expectedEncryption = expectedValues[5]

            // get the current media statistics on the MoonCake
            Endpoint ep = (eps.size() > 0 && eps != null) ? eps[0] : moonCake
            MediaStatistics mediaStatistics = ep.mediaStatistics

            if (mediaStatistics.channels.size() == 0) {
                logger.error("Cannot detect the media statistic channel information")
                assert mediaStatistics.channels.size() != 0
            }

            //Compare the actual values with the expected ones
            MediaChannelStatistics mediaChannelStatistics = mediaStatistics.channels.find { x ->
                x.channelType == channelType.channelType && x.mediaType == channelType.mediaType
            }
            String actualProtocol = mediaChannelStatistics != null ? mediaChannelStatistics.codec : "--"
            String actualResolution = mediaChannelStatistics != null ? mediaChannelStatistics.resolution : "--"
            String actualEncryption = mediaChannelStatistics != null ? mediaChannelStatistics.otherDatas.get("encryption") : "--"
            int actualRate = mediaChannelStatistics != null ? mediaChannelStatistics.rate : -1
            int actualRateUsed = mediaChannelStatistics != null ? mediaChannelStatistics.rateUsed : -1
            int actualFrameRate = mediaChannelStatistics != null ? mediaChannelStatistics.frameRate : -1

            logger.info(String
                    .format("Channel %s actual media statics information is as below:\n" +
                            "1. Actual protocol is: %s\n" +
                            "2. Actual resolution is: %s\n" +
                            "3. Actual rate is: %d%n\n" +
                            "4. Actual rate used is: %d%n\n" +
                            "5. Actual frame rate is: %d%n\n" +
                            "6. Actual encryption is: %s\n",
                            channelType,
                            actualProtocol,
                            actualResolution,
                            actualRate,
                            actualRateUsed,
                            actualFrameRate,
                            actualEncryption))

            assert expectedProtocol == "--" || expectedProtocol.equalsIgnoreCase(actualProtocol) || expectedProtocol.contains(actualProtocol)
            assert expectedResolution == "--" || expectedResolution.equalsIgnoreCase(actualResolution)
            assert expectedRate == "--" || Integer.valueOf(expectedRate) == actualRate
            assert expectedFrameRate == "--" || Integer.valueOf(expectedFrameRate) == actualFrameRate || actualFrameRate > 0
            assert expectedRateUsed == "--" || Integer.valueOf(expectedRateUsed) <= actualRateUsed
            assert expectedEncryption == "--" || expectedEncryption.equalsIgnoreCase(actualEncryption)
        }
        return true
    }

    /**
     * Hang up all endpoints
     *
     * @param eps The endpoint list
     * @return
     */
    def hangUpAll(Endpoint... eps) {
        eps.each { ep -> ep.hangUp() }
        pauseTest(5)
        return true
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

    /**
     * Create random integer in specified range
     *
     * @param min The minimum number
     * @param max The maximum number
     * @return The random integer number
     */
    def getRandomIntegerNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min")
        }
        Random r = new Random()
        return r.nextInt((max - min) + 1) + min
    }

    /**
     * Capture the MoonCake screenshot
     *
     * @param mc The specified MoonCake
     * @return The screenshot location
     */
    def captureScreenShot(MoonCake mc) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
        String location = testContext.logServerAddress.ip + "/${testContext.poolName}/${timeStamp}/" +
                "${testContext.currentCaseFolderRelativePath}/${mc.class.simpleName}_${mc.ip}"
        location = location.replaceAll("\\[", "(").replaceAll("\\]", ")")
        String status = moonCake
                .captureMoonCakeScreen(location,
                        testContext.logServerAddress.username,
                        testContext.logServerAddress.password)
        logger.info('The MoonCake ' + moonCake.ip + ' screenshot has been captured and uploaded onto link: <a href="'
                + status + '">'
                + status + '</a>')
        logger.info("<img src=\"${status}\" height=\"100\" width=\"180\" />")
        return status
    }

    /**
     * Create mac_profile.cfg file on the FTP server
     *
     * @param client The FTP client
     * @param mac The MoonCake mac address
     * @return
     */
    def createConfigOnFtp(FtpClient client, String mac) {
        def result = false
        if (!client.isConnected()) {
            logger.error("ftp server is not connected.")
        } else if (!client.exist('default_mooncake_profile.cfg')) {
            logger.error("Can't find default_mooncake_profile.cfg, please check on ftp server.")
        } else {
            def cfg = "${mac}_profile.cfg"
            try {
                if (!client.exist(cfg)) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
                    client.retrieveFile('default_mooncake_profile.cfg', outputStream)
                    def content = outputStream.toString(StandardCharsets.UTF_8.name())
                    InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name()))
                    client.storeFile(cfg, is)
                }
                result = true
            }
            catch (Exception e) {
                logger.error("createConfigOnFtp", e)
            }
        }
        return result
    }

    /**
     * Remove the mac_profile.cfg file on the FTP server
     *
     * @param client The FTP client
     * @param mac The MoonCake mac address
     * @return
     */
    def deleteConfigOnFtp(FtpClient client, String mac) {
        if (!client.isConnected()) {
            logger.error("ftp server is not connected.")
        } else {
            def cfg = "${mac}_profile.cfg"
            if (client.exist(cfg)) {
                client.remove(cfg)
            }
        }
    }

    def modifyConfigOnFtp(FtpClient client, String sn, Map keyValues) {
        def result = false
        if (!client.isConnected()) {
            logger.error("ftp server is not connected.")
        } else {
            def cfg = "${sn}_profile.cfg"
            ByteArrayOutputStream outputStream = null
            ByteArrayInputStream inputStream = null
            try {
                if (keyValues.containsKey('provision') && client.exist(cfg)) {
                    logger.info("$cfg need to be changed")
                    outputStream = new ByteArrayOutputStream()
                    client.retrieveFile(cfg, outputStream)
                    String content = outputStream.toString(StandardCharsets.UTF_8.name())


                    keyValues['provision'].each { k, v ->
                        logger.info("Change $k to $v")
                        content = updateXmlFile(content, k, v)
                    }
                    inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name()))
                    client.storeFile(cfg, inputStream)
                }
                result = true
            } catch (Exception e) {
                logger.error('modifyConfigOnFtp', e)
            } finally {
                if (outputStream != null) outputStream.close()
                if (inputStream != null) inputStream.close()
            }
        }
        return result
    }

    def updateXmlFile(String content, String tag, String value) {
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(Charset.forName("UTF-8")))
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder()
        Document doc = docBuilder.parse(inputStream)

        //get the specified tag in the XML
        Node element = doc.getElementsByTagName(tag).item(0)

        if (element != null) {
            element.setTextContent(value)
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance()
        Transformer transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        transformer.setOutputProperty(OutputKeys.METHOD, "xml")
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        DOMSource source = new DOMSource(doc)
        StringWriter sw = new StringWriter()
        transformer.transform(source, new StreamResult(sw))

        logger.info("Update the XML content done!")
        return sw.toString()
    }

    /**
     * Send DHCP command on the server
     *
     * @param ip
     * @param command
     * @return
     */
    def doCommandOnHttp(String ip, String command) {
        return new URL("http://$ip:8888/command/$command").text
    }

    /**
     * Encode the input string into URL
     *
     * @param str The string
     * @return The transformed URL string
     */
    def urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20")
    }
}
