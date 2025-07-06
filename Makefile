ACCOUNT_ID := $(shell aws sts get-caller-identity --query Account --output text)
AWS_REGION ?= $(shell aws configure get region)
REPOSITORY ?= mazy-video-tools-api
IMAGE_TAG  ?= latest
ECR_URI    := $(ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/$(REPOSITORY):$(IMAGE_TAG)

build:
	mvn -q clean package -DskipTests

docker-build:
	docker build -t $(REPOSITORY):$(IMAGE_TAG) .

push: docker-build
	aws ecr get-login-password --region $(AWS_REGION) \
	  | docker login --username AWS --password-stdin $(ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com

	docker tag $(REPOSITORY):$(IMAGE_TAG) $(ECR_URI)

	docker push $(ECR_URI)
