import com.polycom.honeycomb.test.context.HoneycombRmServerTestContext

import static com.polycom.honeycomb.test.context.TestContextLoader.config

config(HoneycombRmServerTestContext.class) {
    reportFolderRootPath = "./build/testreports"
    pctcRmServerIp = "172.21.105.55"
    poolName = "MoonCake-Auto-Tests"
    userName = "MoonCakeTester"
}