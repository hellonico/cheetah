function  cheetah() {
	if [ -z "$1" ]
  	then
		curl http://localhost:3000/rest
	else
		curl -Xget "http://localhost:3000/rest/$1"
	fi
}