// Challenge 29: Subtle Time-Based Authentication Bypass
const express = require('express');
const crypto = require('crypto');
const app = express();

app.use(express.json());

// Mock user database
const users = {
    'admin': {
        password: 'super_secret_admin_password_12345',
        role: 'admin',
        lastLogin: null
    },
    'user': {
        password: 'user_password',
        role: 'user', 
        lastLogin: null
    }
};

// Helper function to hash passwords
function hashPassword(password) {
    return crypto.createHash('sha256').update(password).digest('hex');
}

// Store hashed passwords
Object.keys(users).forEach(username => {
    users[username].hashedPassword = hashPassword(users[username].password);
});

// Authentication middleware
function authenticate(req, res, next) {
    const { username, password } = req.body;
    
    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password required' });
    }
    
    const user = users[username];
    if (!user) {
        // Intentional delay to prevent username enumeration
        setTimeout(() => {
            res.status(401).json({ error: 'Invalid credentials' });
        }, 100);
        return;
    }
    
    const hashedInput = hashPassword(password);
    
    // Compare hashes using a custom function
    let isValid = true;
    const userHash = user.hashedPassword;
    const inputHash = hashedInput;
    
    // This looks secure but has a subtle timing vulnerability
    if (userHash.length !== inputHash.length) {
        isValid = false;
    }
    
    for (let i = 0; i < Math.max(userHash.length, inputHash.length); i++) {
        if (userHash[i] !== inputHash[i]) {
            isValid = false;
        }
    }
    
    if (isValid) {
        user.lastLogin = new Date();
        req.user = user;
        req.username = username;
        next();
    } else {
        // Simulate processing time
        setTimeout(() => {
            res.status(401).json({ error: 'Invalid credentials' });
        }, 100);
    }
}

app.post('/login', authenticate, (req, res) => {
    res.json({ 
        message: 'Login successful', 
        role: req.user.role,
        lastLogin: req.user.lastLogin 
    });
});

app.post('/admin', authenticate, (req, res) => {
    if (req.user.role === 'admin') {
        res.json({ 
            message: 'Welcome admin!', 
            users: Object.keys(users),
            serverInfo: process.env 
        });
    } else {
        res.status(403).json({ error: 'Admin access required' });
    }
});

app.listen(3000, () => {
    console.log('Server running on port 3000');
});
