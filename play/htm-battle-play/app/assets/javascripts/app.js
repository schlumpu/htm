'use strict';

angular.module('htm', ['common.playRoutes', 'common.filters', 'htm.services', 'ui.bootstrap']).
controller('BattleCtrl', ['$scope', '$timeout', 'appService', BattleCtrl]).
controller('PoolsCtrl', ['$scope', '$timeout', 'playRoutes', 'appService', PoolsCtrl]);