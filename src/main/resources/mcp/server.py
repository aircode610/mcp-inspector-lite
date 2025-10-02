from mcp.server.fastmcp import FastMCP

# Server
mcp = FastMCP("Demo")


# Tools
@mcp.tool()
def add(a: int, b: int) -> int:
    """Add two numbers"""
    return a + b

@mcp.tool()
def multiply(a: int, b: int) -> int:
    """Multiply two numbers"""
    return a * b

@mcp.tool()
def word_count(text: str) -> Dict[str, int]:
    """Count words in the given text"""
    words = text.split()
    return {"word_count": len(words)}

@mcp.tool()
def fibonacci(n: int) -> List[int]:
    """Return the Fibonacci sequence up to n terms"""
    seq = [0, 1]
    for _ in range(2, n):
        seq.append(seq[-1] + seq[-2])
    return seq[:n]

@mcp.tool()
def ping() -> str:
    """Simple health check"""
    return "pong"


# Resources
@mcp.resource("greeting://{name}")
def get_greeting(name: str) -> str:
    """Get a personalized greeting"""
    return f"Hello, {name}!"

@mcp.resource("quote://{category}")
def get_quote(category: str) -> str:
    """Return a simple quote based on category"""
    quotes = {
        "inspiration": "The best way to get started is to quit talking and begin doing.",
        "humor": "I'm not arguing, I'm just explaining why I'm right.",
        "wisdom": "Knowledge speaks, but wisdom listens.",
    }
    return quotes.get(category.lower(), "No quote found for this category.")


# Prompts
@mcp.prompt()
def greet_user(name: str, style: str = "friendly") -> str:
    """Generate a greeting prompt"""
    styles = {
        "friendly": "Please write a warm, friendly greeting",
        "formal": "Please write a formal, professional greeting",
        "casual": "Please write a casual, relaxed greeting",
    }

    return f"{styles.get(style, styles['friendly'])} for someone named {name}."

@mcp.prompt()
def summarize_text(text: str, style: str = "short") -> str:
    """Generate a summary prompt instruction"""
    if style == "short":
        return f"Summarize this text in one sentence: {text}"
    elif style == "detailed":
        return f"Write a detailed summary of this text: {text}"
    else:
        return f"Summarize this text in a {style} way: {text}"


# Run server over stdio
if __name__ == "__main__":
    mcp.run(transport='stdio')