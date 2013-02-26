require('./integration_config.js');
var frisby = require('frisby');
var utils = require('./utils.js');

var channelName = utils.randomChannelName();

utils.runInTestChannel(channelName, function (channelResponse) {
    var channelResource = channelResponse['_links']['self']['href'];
    frisby.create('Inserting a first item')
        .post(channelResource, null, { body: "FIRST ITEM"})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(200)
        .afterJSON(function (response) {
            var firstItemUrl = response['_links']['self']['href'];
            frisby.create('Verifying that first channel item doesnt have a previous')
                .get(firstItemUrl)
                .expectStatus(200)
                .after(function (err, res, body) {
                    for (var item in res.headers) {
                        if (item == "link") {
                            expect(res.headers[item]).not.toContain("previous");
                        }
                    }
                    frisby.create("Inserting a second item")
                        .post(channelResource, null, {body: "SECOND ITEM"})
                        .addHeader("Content-Type", "text/plain")
                        .expectStatus(200)
                        .afterJSON(function (response) {
                            var secondItemUrl = response['_links']['self']['href'];
                            frisby.create("Checking the Link header.")
                                .get(secondItemUrl)
                                .expectStatus(200)
                                .expectHeader("link", "<" + firstItemUrl + ">;rel=\"previous\"")
                                .toss()
                        })
                        .toss();
                })
                .toss();
        })
        .toss();
});

