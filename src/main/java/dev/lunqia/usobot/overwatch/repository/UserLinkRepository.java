package dev.lunqia.usobot.overwatch.repository;

import dev.lunqia.usobot.overwatch.model.UserLink;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserLinkRepository extends ReactiveMongoRepository<UserLink, String> {}
