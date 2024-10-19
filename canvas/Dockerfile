FROM ruby:2.7

RUN apt update -y; apt -y install wget zlib1g-dev libxml2-dev libsqlite3-dev libpq-dev libxmlsec1-dev curl build-essential libidn11-dev

RUN curl -sL https://deb.nodesource.com/setup_16.x | bash -
RUN apt update -y; apt -y install nodejs

RUN npm install -g npm@latest

RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add -
RUN bash -c 'echo "deb https://dl.yarnpkg.com/debian/ stable main" > /etc/apt/sources.list.d/yarn.list'
RUN apt update -y; apt -y install yarn

WORKDIR /home
RUN wget -O canvas.tar.gz https://codeload.github.com/instructure/canvas-lms/legacy.tar.gz/refs/heads/prod
RUN tar -xvf canvas.tar.gz
RUN rm canvas.tar.gz
RUN mv $(ls -d */|grep instructure|head -n 1) canvas
WORKDIR /home/canvas

RUN gem install bundle; gem install bundler:2.2.33

RUN bundle install
RUN yarn install --pure-lockfile

RUN cp /home/canvas/config/amazon_s3.yml.example /home/canvas/config/amazon_s3.yml \
	&& cp /home/canvas/config/delayed_jobs.yml.example /home/canvas/config/delayed_jobs.yml \
	&& cp /home/canvas/config/domain.yml.example /home/canvas/config/domain.yml \
	&& cp /home/canvas/config/file_store.yml.example /home/canvas/config/file_store.yml \
	&& cp /home/canvas/config/outgoing_mail.yml.example /home/canvas/config/outgoing_mail.yml \
	&& cp /home/canvas/config/security.yml.example /home/canvas/config/security.yml \
	&& cp /home/canvas/config/external_migration.yml.example /home/canvas/config/external_migration.yml \
	&& cp /home/canvas/config/dynamic_settings.yml.example /home/canvas/config/dynamic_settings.yml

COPY database.yml /home/canvas/config/database.yml
COPY redis.yml /home/canvas/config/redis.yml
COPY cache_store.yml /home/canvas/config/cache_store.yml


#RUN BUNDLE_JOBS=1 bundle exec rails canvas:compile_assets
WORKDIR /home/canvas/public
COPY dist.zip /home/canvas/public/dist.zip
COPY doc.zip /home/canvas/public/doc.zip

RUN unzip dist.zip
RUN unzip doc.zip
RUN rm dist.zip; rm doc.zip

WORKDIR /home/canvas

EXPOSE 3000/tcp

ENTRYPOINT ["bundle", "exec", "rails", "server", "--binding=0.0.0.0"]
