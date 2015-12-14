var load = function (caseIds) {
    var caseIdsCond = StringUtils.join(Arrays.asList(StringUtils.split(caseIds, ',')).stream().map(function (i) {
        return '"' + i + '"'
    }).toArray(), ',');

    query(function (cb) {
        with (imports) {
            return new CaseLinksQuery('caseId in [' + caseIdsCond + ']', cb)
        }
    }).then(function (cases) {
        emit('all_links', cases);
    }).finally(function () {
        done();
    });
};

var map = function (k, v) {
    for (var idx = 0; idx < v.length; idx++) {
        emit(v[idx].caseNumber(), {
            count: 1
        });
    }
};

var reduce = function (k, v) {
    try {
        var reducedVal = {
            count: 0
        };
        for (var idx = 0; idx < v.length; idx++) {
            reducedVal.count += v[idx].count;
        }
        emit(k, reducedVal.count);
    } catch (err) {
        log.error(err);
    }
};

var finalize = function (data) {
    emitM(data);
};
