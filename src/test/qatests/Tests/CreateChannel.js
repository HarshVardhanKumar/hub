/**
 * Created with JetBrains WebStorm.
 * User: gnewcomb
 * Date: 3/1/13
 * Time: 9:47 AM
 * To change this template use File | Settings | File Templates.
 */

// CREATE CHANNEL tests

var chai = require('chai'),
    expect = chai.expect,
    superagent = require('superagent'),
    request = require('request'),
    moment = require('moment'),
    async = require('async'),
    lodash = require('lodash');

var dhh = require('.././DH_test_helpers/DHtesthelpers.js'),
    gu = require('../genericUtils.js'),
    ranU = require('../randomUtils.js');

var DOMAIN = dhh.DOMAIN,
    // DOMAIN = '10.11.15.162:8080',   // crypto proxy
    DEBUG = true;


describe('Create Channel: ', function(){

    var channelName;

    var checkChannelBodyStructure = function(body) {
        expect(body.hasOwnProperty('_links')).to.be.true;
        expect(body._links.hasOwnProperty('self')).to.be.true;
        expect(body._links.self.hasOwnProperty('href')).to.be.true;
        expect(body._links.hasOwnProperty('latest')).to.be.true;
        expect(body._links.latest.hasOwnProperty('href')).to.be.true;
        expect(body._links.hasOwnProperty('ws')).to.be.true;
        expect(body._links.ws.hasOwnProperty('href')).to.be.true;
        expect(body.hasOwnProperty('name')).to.be.true;
        expect(body.hasOwnProperty('creationDate')).to.be.true;
        expect(body.hasOwnProperty('ttlDays')).to.be.true;
        expect(body.hasOwnProperty('type')).to.be.true;
        expect(body.hasOwnProperty('contentSizeKB')).to.be.true;
        expect(body.hasOwnProperty('peakRequestRateSeconds')).to.be.true;
        expect(body.hasOwnProperty('ttlMillis')).to.be.true;
        expect(body.hasOwnProperty('description')).to.be.true;

        expect(lodash.keys(body).length).to.equal(9);
        expect(lodash.keys(body._links).length).to.equal(4);
    }

    before(function(myCallback){
        channelName = dhh.getRandomChannelName();
        dhh.createChannel({name: channelName, domain: DOMAIN}, function(res){
            if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                //console.log('bad things');
                throw new Error(res.error);
            }
            console.log('Main test channel:'+ channelName);
            myCallback();
        });
    });

    describe('Acceptance, TTL not specified', function() {

        var createRes,
            channelUri,
            acceptName;

        before(function(done) {

            acceptName = dhh.getRandomChannelName();

            dhh.createChannel({name: acceptName, debug: false, domain: DOMAIN}, function(res, uri) {
                createRes = res;
                channelUri = uri;
                gu.debugLog('create result status: '+ createRes.status);

                done();
            });
        })

        it('channel creation returns 201 (Created)', function(){
            expect(createRes.status).to.equal(gu.HTTPresponses.Created);
        });

        it('channel uri is correct', function(done) {

            dhh.getChannel({'uri': channelUri}, function(res) {
                expect(res.status).to.equal(gu.HTTPresponses.OK);
                expect(res.body._links.self.href).to.equal(channelUri);

                done();
            })
        })

        it('creation response has correctly structured body', function() {
            checkChannelBodyStructure(createRes.body);
        })

        it('creation response includes correct Location header', function() {
            expect(createRes.headers.hasOwnProperty('location'));
            expect(createRes.headers.location).to.equal(channelUri);
        })

        it('self link is correct', function() {
            expect(createRes.body._links.self.href).to.equal(channelUri);
        })

        // NOTE: _links.latest and _links.ws have their own tests
        it('name is correct', function() {
            expect(createRes.body.name).to.equal(acceptName);
        })

        it('creationDate is correct', function() {
            var returnedDate = moment(createRes.body.creationDate);

            expect(returnedDate.add('minutes', 5).isAfter(moment())).to.be.true;
        })

        it('TTL has numeric value', function() {
            expect(lodash.isNumber(createRes.body.ttlMillis)).to.be.true;
        })

        // https://www.pivotaltracker.com/story/show/50668987
        it('TTL defaults to 120 days', function() {
            expect(createRes.body.ttlMillis).to.equal(10368000000);
        })
    })

    describe('Acceptance with TTL set', function() {
        var createRes,
            channelUri,
            acceptName,
            acceptTTL = 100;

        before(function(done) {

            acceptName = dhh.getRandomChannelName();

            dhh.createChannel({name: acceptName, ttlDays: acceptTTL, domain: DOMAIN}, function(res, uri) {
                createRes = res;
                channelUri = uri;

                done();
            });
        })

        it('channel creation returns 201 (Created)', function(){
            expect(createRes.status).to.equal(gu.HTTPresponses.Created);
        });

        it('creation response has correctly structured body', function() {
            checkChannelBodyStructure(createRes.body);
        })

        it('has correct value for TTL', function() {
            var cnMetadata = new dhh.channelMetadata(createRes.body),
                actualTTL = cnMetadata.getTTL();
            expect(actualTTL).to.equal(acceptTTL);
        })
    })

    describe('Positive parameter tests', function() {

        describe('name', function() {
            it('may contain underscores', function(done) {
                var name = dhh.getRandomChannelName(5) +'_'+ dhh.getRandomChannelName(5) + '___'+ dhh.getRandomChannelName(5) + '_';

                dhh.createChannel({name: name, domain: DOMAIN}, function(res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Created);

                    done();
                })
            })
        })

        describe('TTL', function() {

            // BUG: https://www.pivotaltracker.com/story/show/52425747
            it('may be null - results in no ttlDays property being returned', function(done) {
                var name = dhh.getRandomChannelName();

                dhh.createChannel({name: name, ttlDays: null, debug: true, domain: DOMAIN}, function(res) {
                    expect(res.body.hasOwnProperty('ttlDays')).to.be.false;

                    done();
                })
            })
        })
    })

    describe('Error cases', function() {

        describe('name', function() {

            var badNameYieldsBadRequest = function(cnName, callback) {
                dhh.createChannel({name: cnName, domain: DOMAIN}, function(res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);

                    callback();
                })
            }

            it.skip('may not match a word reserved by Cassandra', function(done) {
                channelName = 'channelMetadata';

                dhh.createChannel({name: channelName, domain: DOMAIN}, function(res){
                    if ((res.error) || (!gu.isHTTPSuccess(res.status))) {
                        //console.log('bad things');
                        throw new Error(res.error);
                    }
                    gu.debugLog('Main test channel:'+ channelName);

                    done();
                });
            })

            // See:  https://www.pivotaltracker.com/story/show/49566971
            it('may not be blank', function(done){
                badNameYieldsBadRequest('', done);
            });

            it('cannot consist only of whitespace', function(done) {
                badNameYieldsBadRequest('    ', done);
            })

            // https://www.pivotaltracker.com/story/show/51434073 -
            it('whitespace is trimmed from name', function(done) {
                var name = dhh.getRandomChannelName();

                dhh.createChannel({name: '  '+ name +'  ', domain: DOMAIN}, function(res, uri) {
                    expect(res.status).to.equal(gu.HTTPresponses.Created);

                    dhh.getChannel({uri: uri}, function(getRes, body) {
                        expect(getRes.status).to.equal(gu.HTTPresponses.OK);
                        expect(body.name).to.equal(name);

                        done();
                    })
                })
            })

            // https://www.pivotaltracker.com/story/show/51434189
            it('cannot contain a forward slash', function(done) {
                var name = dhh.getRandomChannelName(10) +'/'+ dhh.getRandomChannelName(10);

                badNameYieldsBadRequest(name, done);
            })

            it('cannot contain a space', function(done) {
                var name = dhh.getRandomChannelName(10) +' '+ dhh.getRandomChannelName(10);

                badNameYieldsBadRequest(name, done);
            })

            it('may not contain a dash', function(done){
                var name = dhh.getRandomChannelName(10) +'-'+ dhh.getRandomChannelName(10);

                badNameYieldsBadRequest('', done);
            });

            it('may not contain upper ASCII characters chosen at random (161 - 447)', function(done) {
                var name = ranU.randomString(26, ranU.extendedCharOnly);
                gu.debugLog('Name: '+ name);

                badNameYieldsBadRequest(name, done);
            })
        })

        describe('Code not implemented - TTL', function() {

            var badTTLYieldsBadRequest = function(TTL, callback) {
                dhh.createChannel({name: dhh.getRandomChannelName(), ttlMillis: TTL, domain: DOMAIN}, function(res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);

                    callback();
                })
            }

            it.skip('BUG: https://www.pivotaltracker.com/story/show/52486795 - may not be negative', function(done) {
                badTTLYieldsBadRequest(-500, done);
            })

            it.skip('BUG: https://www.pivotaltracker.com/story/show/52486795 - may not be zero', function(done) {
                badTTLYieldsBadRequest(0, done);
            })

            // TODO: blank or empty --> null, or disallowed?

            it.skip('BUG: https://www.pivotaltracker.com/story/show/52486795 - may not be alpha characters', function(done) {
                badTTLYieldsBadRequest('ohai', done);
            })

            it.skip('BUG: https://www.pivotaltracker.com/story/show/52486795 - may not contain a period', function(done) {
                badTTLYieldsBadRequest(8675.309, done);
            })
        })


        // https://www.pivotaltracker.com/story/show/46667409
        it('no / empty payload not allowed', function(done) {

            superagent.agent().post(dhh.URL_ROOT +'/channel')
                .set('Content-Type', 'application/json')
                .send('')
                .end(function(err, res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Bad_Request);
                    gu.debugLog('Response status: '+ res.status, DEBUG);

                    done();
                });
        });

        describe('channel names must be unique', function() {

            it.skip('BUG: https://www.pivotaltracker.com/story/show/52507013 - parallel attempts to create channel with same name only allow one to be created', function(done) {
                var name = dhh.getRandomChannelName(),
                    numAttempts = 10,
                    VERBOSE = true;

                var makeChannel = function(index, callback) {
                    dhh.createChannel({name: name, domain: DOMAIN}, function(res, channelUri) {
                        gu.debugLog('channel creation attempt result: '+ res.status, VERBOSE);

                        if (!lodash.contains([gu.HTTPresponses.Created, gu.HTTPresponses.Conflict], res.status)) {
                            callback(res.status, null);
                        }
                        else {
                            callback(null, {status: res.status, uri: channelUri});
                        }
                    })
                }

                async.times(numAttempts, function(n, next) {
                    makeChannel(n, function(err, makeResponse) {
                        next(err, makeResponse);
                    })
                }, function(err, makeResponses) {
                    if (null != err) {
                        gu.debugLog('Error, unexpected response: '+ err);
                        expect(err).to.be.null;
                    } else {
                        var numCreateResponses = lodash.countBy(makeResponses, {status: gu.HTTPresponses.Created}).true,
                            numConflictResponses = lodash.countBy(makeResponses, {status: gu.HTTPresponses.Conflict}).true;

                        expect(numCreateResponses).to.equal(1);
                        expect(numConflictResponses).to.equal(numAttempts - 1);
                    }

                    done();
                })
            })

            // https://www.pivotaltracker.com/story/show/44113267
            it('return 409 if attempting to create channel with a name already in use', function(done) {
                dhh.createChannel({name: channelName, domain: DOMAIN}, function(res) {
                    expect(res.status).to.equal(gu.HTTPresponses.Conflict);

                    done();
                });
            });

            it('can create two channels whose names differ only in case', function(done) {
                var base = dhh.getRandomChannelName(),
                    firstName = base +'z',
                    secondName = base +'Z';

                dhh.createChannel({name: firstName, domain: DOMAIN}, function(firstRes) {
                    expect(firstRes.status).to.equal(gu.HTTPresponses.Created);

                    dhh.createChannel({name: secondName, domain: DOMAIN}, function(secondRes) {
                        expect(secondRes.status).to.equal(gu.HTTPresponses.Created);

                        done();
                    })
                })
            })
        })
    })



});