package net.minecraft.paper.java;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

public interface RepositoryLibraryLoader {
    RepositorySystem repository();
    DefaultRepositorySystemSession session();
    List<RemoteRepository> repositories();
}
