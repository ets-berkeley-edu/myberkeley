#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'rubygems'
require 'json'
require 'digest/sha1'
require 'logger'
require 'sling/sling'
require 'sling/users'
require 'sling/authz'
include SlingInterface
include SlingUsers

module MyBerkeleyData
  BASIC_PROFILE_PROPS = [
    'email', 'firstName', 'lastName', 'role', 'department', 'college', 'major'
  ]
  UG_GRAD_FLAG_MAP = {:U => 'Undergraduate Student', :G => 'Graduate Student'}
  STUDENT_ROLES = ["Undergraduate Student", "Graduate Student", "Student"]
  COLLEGE_ABBR_TO_PROFILE = { "ENV DSGN" => "College of Environmental Design", "NAT RES" => "College of Natural Resources" }
  ENV_PROD = 'prod'
  
  # Test data for development environments
  TEST_ADVISER_GROUPS = ["myb-advisers-ced-students", "myb-advisers-cnr-undergrads", "myb-advisers-cnr-grads-agricultural",
    "myb-advisers-cnr-grads-environmental", "myb-advisers-cnr-grads-nutritional", "myb-advisers-cnr-grads-plant"]
  TEST_DEMOGRAPHICS = [{
      "college" => "ENV DSGN",
      "undergradMajors" => [ "ARCHITECTURE", "INDIVIDUAL", "LIMITED", "LANDSCAPE ARCH", "URBAN STUDIES" ],
      "gradMajors" => [ "ARCHITECTURE", "CITY REGIONAL PLAN", "DESIGN", "LIMITED", "LAND ARCH AND ENV PLAN", "URBAN DESIGN" ]
    }, {
      "college" => "NAT RES",
      "undergradMajors" => [ "AGR & RES ECON", "CONSERV&RSRC STUDIES", "ENV ECON & POLICY", "ENVIR SCIENCES", "FOREST & NATURAL RES", "GENETICS & PLANT BIO", "MICROBIAL BIOLOGY", "MOL ENV BIOLOGY", "MOLECULAR TOXICOLOGY", "NUTR SCI-DIETETICS", "NUTR SCI-PHYS & MET", "NUTRITION SCIENCE", "SOCIETY&ENVIRONMENT", "UNDECLARED", "VISTOR-NON-UC CAMPUS" ],
      "gradMajors" => [ "AGR & RES ECON", "AGRICULTURAL CHEM", "COMP BIOCHEMISTRY", "ENV SCI POL AND MGMT", "FORESTRY", "PLANT BIOLOGY", "RANGE MANAGEMENT" ]
    }]
  CALNET_TEST_USER_IDS = ["test-300846","test-300847","test-300848","test-300849","test-300850","test-300851",
    "test-300852","test-300853","test-300854","test-300855","test-300856","test-300857","test-300858",
    "test-300859","test-300860","test-300861","test-300862","test-300863","test-300864","test-300865",
    "test-300866","test-300867","test-300868","test-300869","test-300870","test-300871","test-300872",
    "test-300873","test-300874","test-300875","test-300876","test-300877"]

  class UcbDataLoader
    TEST_USER_PREFIX = 'testuser'

    @env = nil

    @bedeworkServer = nil
    @bedeworkPort = nil

    @user_manager = nil
    @sling = nil
    @authz = nil

    def initialize(server, admin_password="admin", bedeworkServer=nil, bedeworkPort="8080")
      @log = Logger.new(STDOUT)
      @log.level = Logger::DEBUG
      @sling = Sling.new(server, true)
      @user_manager = UserManager.new(@sling)
      real_admin = User.new("admin", admin_password)
      @sling.switch_user(real_admin)
      @sling.do_login
      @authz = SlingAuthz::Authz.new(@sling)
      @server = server
      @bedeworkServer = bedeworkServer
      @bedeworkPort = bedeworkPort
    end

    def make_password(name, key)
      digest = Digest::SHA1.new
      digest.update(name).update(key)
      return digest.hexdigest
    end

    def get_all_ucb_accounts
      res = @sling.execute_get(@sling.url_for("system/myberkeley/integratedUserIds.json"))
      if (res.code != "200")
        @log.error("Could not get existing integrated users: #{res.code} / #{res.body}")
        return nil
      else
        return (JSON.parse(res.body))["userIds"]
      end
    end

    def add_user_to_group(user_id, group_id)
      result = @sling.execute_post(@sling.url_for("/system/userManager/group/#{group_id}.update.html"), {
        ":member" => user_id
      })
      @log.error("#{result.code} / #{result.body}") if (result.code.to_i > 299)
    end

    def load_dev_advisers
      all_data = JSON.load(File.open "dev_advisers_json.js", "r")
      users = all_data['users']
      loaded_users = Array.new
      users.each do |user|
        loaded_user = load_defined_adviser user
        puts "loaded user: #{loaded_user.inspect}"
        if (loaded_user)
          TEST_ADVISER_GROUPS.each do |group_id|
            add_user_to_group(loaded_user.name, group_id)
          end
          loaded_users << loaded_user
        end
      end
      return loaded_users
    end

    def load_defined_adviser user
        username = user[0]
        user_props = user[1]
        make_adviser_props user_props
        # This will return nil if the user already exists.
        loaded_user = create_user_with_props username, user_props
        return loaded_user
    end

    def make_adviser_props user_props #need to have firstName, lastName and email loaded already
        user_props['role'] = ['Staff']
    end

    def load_calnet_test_users
      i = 0
      CALNET_TEST_USER_IDS.each do |id|
        first_name = id.split('-')[0].to_s
        last_name = id.split('-')[1].to_s
        uid = id.split('-')[1].to_s
        # for a user like test-212381, the calnet uid will be 212381
        user_props = generate_student_props uid, first_name, last_name, i, CALNET_TEST_USER_IDS.length
        loaded_calnet_test_user = load_user uid, user_props
        apply_demographic uid, user_props
        i = i + 1
      end
    end

    def generate_student_props(username, first_name, last_name, index, length)
        user_props = {}
        user_props[':name'] = username
        user_props['firstName'] = first_name.chomp
        user_props['lastName'] = last_name.chomp
        user_props['email'] = first_name.downcase + '.' + last_name.downcase + '@berkeley.edu'
        demog = TEST_DEMOGRAPHICS[index % TEST_DEMOGRAPHICS.length]
        user_props['college'] = COLLEGE_ABBR_TO_PROFILE[demog["college"]]
        if (index < length/2)
          user_props['role'] = UG_GRAD_FLAG_MAP[:U]
          majors_demog = demog["undergradMajors"]
          user_props['major'] = majors_demog[index % majors_demog.length].sub(/&/, 'AND')
          user_props['myb-demographics'] = [
            "/colleges/#{demog['college']}/standings/undergrad",
            "/colleges/#{demog['college']}/standings/undergrad/majors/" + user_props['major']
          ]
        else
          user_props['role'] = UG_GRAD_FLAG_MAP[:G]
          majors_demog = demog["gradMajors"]
          user_props['major'] = majors_demog[index % majors_demog.length].sub(/&/, 'AND')
          user_props['myb-demographics'] = [
            "/colleges/#{demog['college']}/standings/grad",
            "/colleges/#{demog['college']}/standings/grad/programs/" + user_props['major']
          ]
          if (index == length - 1)
            user_props['myb-demographics'].push("/colleges/#{demog['college']}/standings/grad/programs/DOUBLE")
            user_props['myb-demographics'].push("/colleges/#{demog['college']}/standings/grad/programs/PSYCHOLOGY")
            # Basic Profile only handles single-valued string properties
            user_props['major'] = "DOUBLE : " + user_props['major'] + " ; " + "PSYCHOLOGY"
          end
        end
        return user_props
    end

    def create_user_with_props(username, user_props, password=nil)
      @log.info "Creating user: #{username}, props: #{user_props.inspect}"
      if (password)
        user = User.new(username, password)
      else
        user = User.new(username)
      end
      data = { ":name" => user.name,
              "pwd" => user.password,
              "pwdConfirm" => user.password
      }
      firstname = user_props['firstName']
      lastname = user_props['lastName']
      if (!firstname.nil? and !lastname.nil?)
        data = add_profile_property(user_props, data)
        data[":sakai:pages-template"] = "/var/templates/site/defaultuser"
      end
      result = @sling.execute_post(@sling.url_for("#{$USER_URI}"), data)
      if (result.code.to_i > 299)
        @log.info "Error creating user"
        return nil
      end
      if (STUDENT_ROLES.include?(user_props["role"]))
        apply_student_aces(user)
      else
        apply_adviser_aces(user)
      end
      apply_demographic(username, user_props)
      create_bedework_acct(username)
      return user
    end

    def add_profile_property(user_props, post_data)
      BASIC_PROFILE_PROPS.each do |prop|
        post_data[prop] = user_props[prop] if (!user_props[prop].nil?)
      end
      return post_data
    end

    def update_profile_properties(sling, target_user, user_props)
      data = {}
      return target_user.update_properties(sling, add_profile_property(user_props, data))
    end

    def load_user(username, user_props, password=nil)
      target_user = create_user_with_props username, user_props, password

      # if user exists, they will not be (re)created but props may be updated
      if (target_user.nil?)
        puts "user #{username} not created, may already exist, attempting to update properties of user: #{user_props.inspect}"
        target_user = update_user(username, user_props)
      end
      return target_user
    end

    def update_user(username, user_props, password=nil)
      if (password)
          target_user = User.new username, password
        else
          target_user = User.new username
        end
      response = update_profile_properties @sling, target_user, user_props
      if (response.code.to_i > 299)
        @log.error("Could not update user #{username}: #{response.code}, #{response.body}")
        return nil
      end
      apply_demographic(username, user_props)
      create_bedework_acct(username)
      return target_user
    end

    def create_bedework_acct(username)
      # create users on the bedework server.
      # this will only work if the server has been put into unsecure login mode.
      if (@bedeworkServer)
        puts "Creating a bedework account for user #{username} on server #{@bedeworkServer}..."
        Net::HTTP.start(@bedeworkServer, @bedeworkPort) { |http|
          req = Net::HTTP::Options.new('/ucaldav/principals/users/' + username)
          req.basic_auth username, username
          response = http.request(req)
        }
      end
    end

    def apply_student_aces(student)
      home_url = @sling.url_for(student.home_path_for @sling)
      result = @sling.execute_post("#{home_url}.modifyAce.html", {
        "principalId" => "everyone",
        "privilege@jcr:read" => "denied"
      })
      @log.error("#{result.code} / #{result.body}") if (result.code.to_i > 299)
      result = @sling.execute_post("#{home_url}.modifyAce.html", {
        "principalId" => "anonymous",
        "privilege@jcr:read" => "denied"
      })
      @log.error("#{result.code} / #{result.body}") if (result.code.to_i > 299)
    end

    def apply_adviser_aces(adviser)
      home_url = @sling.url_for(adviser.home_path_for @sling)
      result = @sling.execute_post("#{home_url}.modifyAce.html", {
        "principalId" => "anonymous",
        "privilege@jcr:read" => "denied"
      })
      @log.error("#{result.code} / #{result.body}") if (result.code.to_i > 299)
    end

    def apply_demographic(uid, user_props)
      demog = user_props["myb-demographics"]
      if (!demog.nil?)
        if (demog.empty?)
          result = @sling.execute_post("#{@server}~#{uid}.myb-demographic.html", {
            "myb-demographics@Delete" => ""
          })
        else
          result = @sling.execute_post("#{@server}~#{uid}.myb-demographic.html", {
            "myb-demographics" => demog
          })
        end
        @log.error("#{result.code} / #{result.body}") if (result.code.to_i > 299)
      end
    end
  end
end

if ($PROGRAM_NAME.include? 'ucb_data_loader.rb')
  puts "will load data on server #{ARGV[0]}"
  sdl = MyBerkeleyData::UcbDataLoader.new ARGV[0], ARGV[1], ARGV[2], ARGV[3]
  sdl.load_dev_advisers
  sdl.load_calnet_test_users
end