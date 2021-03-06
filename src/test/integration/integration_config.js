frisby = require('frisby');
utils = require('./utils.js');
ip = require('ip');
var _ = require('lodash');

hubDomain = process.env.hubDomain;
satelliteDomain = process.env.satelliteDomain;
runEncrypted = process.env.runEncrypted || false;
integrationTestPath = process.env.integrationTestPath || 'src/test/integration/';
callbackPort = runEncrypted = process.env.callbackPort || 8888;

//this does not report the correct ip address when connected via the vpn
//override ipAddress in integration_config_local.js
ipAddress = ip.address();

// Try to load a local file if it exists. Allows folks to trump default values defined above.
try {
	require('./integration_config_local.js');
}
catch ( err ){}

if (_.startsWith(hubDomain, 'http')) {
    hubUrlBase = hubDomain;
} else {
    hubUrlBase = 'http://' + hubDomain;
}

if (_.startsWith(satelliteDomain, 'http')) {
    satelliteUrl = satelliteDomain;
} else {
    satelliteUrl = 'http://' + satelliteDomain;
}

channelUrl = hubUrlBase + '/channel';
callbackDomain = 'http://' + ipAddress;
stableOffset = 5;

console.log("hubDomain " + hubDomain);
console.log("satelliteDomain " + satelliteDomain);
console.log("runEncrypted " + runEncrypted);
console.log("callbackDomain " + callbackDomain);

jasmine.getEnv().defaultTimeoutInterval = 60 * 1000;
