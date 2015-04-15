(function(){
	'use strict';

	/* Services */
	
	var api  = '/api/v3/';

	angular.module('htm.api', ['ngResource'])
	
		.factory('Tournament', ['$resource', function($resource){
			return $resource(api + 'tournament/:id', { "id" : "@id" });
		}])
		.factory('Fight', ['$resource', function($resource) {
			return $resource(api + 'tournament/:id/fight/:fightId', { "id" : "@id", "fightId":"@fightId" });
		}])		
		.factory('Fighter', ['$resource', function($resource) {
			return $resource(api + 'tournament/:id/fighter/:fighterNumber', { "id" : "@id", "fighterNumber":"@fighterNumber" });
		}])
		.factory('Phase', ['$resource', function($resource) {
			return $resource(api + 'tournament/:id/phase', { "id" : "@id"});
		}])
		.factory('Participant', ['$resource', function($resource){
			return $resource(api + 'participant/:id', { "id" : "@id" }, 
				{ 
					update: { method: 'PUT' },
					postPicture: { 
						method: 'POST',
						params:{},
						url: api+'participant/picture/:id',
						transformRequest: function(data){
							    var fd = new FormData();
							   	fd.append('file',data.file);
							    return fd;
						},
						headers:{'Content-Type': undefined}
					}
				}
			);
		}])

		.factory('Country', ['$resource', function($resource) {
  			return $resource(api + 'country');
		}])

		.factory('Club', ['$resource', function($resource) {
  			return $resource(api + 'club');
		}])

		.factory('Statistics', ['$resource', function($resource) {
  			return $resource(api+'participant/totals');
		}])

		;

})();